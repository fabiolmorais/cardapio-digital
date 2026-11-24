package br.com.cardapio.adapter.in.web.report;

import br.com.cardapio.application.port.in.report.MenuFull;
import br.com.cardapio.application.port.in.report.MenuProduct;
import br.com.cardapio.domain.report.model.AttentionPoint;
import br.com.cardapio.domain.report.model.LeftInCart;
import br.com.cardapio.domain.report.model.MenuInterest;
import br.com.cardapio.domain.report.model.ProductInterest;
import br.com.cardapio.domain.report.model.OptionSales;
import br.com.cardapio.domain.report.model.ProductPair;
import br.com.cardapio.domain.report.model.VariantSales;
import br.com.cardapio.domain.report.model.VideoComparison;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

/** A tabela de produtos com o que o Business vê a mais, e os blocos completos do Cardápio. */
final class MenuFullDtos {

    private MenuFullDtos() {
    }

    enum Abc { A, B, C }

    @Schema(name = "ReportMenuProduct", description = """
            Um produto no período (sem os cancelados). No Business, também a curva ABC, o faturamento do
            período anterior e as unidades canceladas; no Pro, esses três vêm null.""")
    record Row(@Schema(requiredMode = REQUIRED) UUID productId, @Schema(requiredMode = REQUIRED) String name,
               @Schema(requiredMode = REQUIRED) String category, @Schema(requiredMode = REQUIRED) int quantity,
               @Schema(requiredMode = REQUIRED) long revenueCents, Abc abc, Long comparedRevenueCents,
               Integer canceled) {
        static Row of(MenuProduct p) {
            var s = p.sales();
            return new Row(s.productId().value(), s.name(), s.category(), s.quantity(), s.revenue().cents(),
                    p.abc() == null ? null : Abc.valueOf(p.abc().name()),
                    p.compared() == null ? null : p.compared().cents(), p.canceled());
        }
    }

    @Schema(name = "ReportCanceledProduct", description = "Um dos mais cancelados, com o motivo que se repetiu")
    record Canceled(@Schema(requiredMode = REQUIRED) String name, @Schema(requiredMode = REQUIRED) int times,
                    String reason, Integer reasonTimes) {
        static Canceled of(AttentionPoint.CanceledProduct c) {
            return new Canceled(c.name(), c.times(), c.reason(), c.reason() == null ? null : c.reasonTimes());
        }
    }

    @Schema(name = "ReportSize", description = "Um tamanho do produto e quantas unidades vendeu")
    record Size(@Schema(requiredMode = REQUIRED) String name, @Schema(requiredMode = REQUIRED) int quantity) {
    }

    @Schema(name = "ReportProductSizes", description = "Um produto com os tamanhos, do mais escolhido")
    record ProductSizes(@Schema(requiredMode = REQUIRED) String product,
                        @Schema(requiredMode = REQUIRED) List<Size> sizes) {
        static ProductSizes of(List<VariantSales> sizes) {
            return new ProductSizes(sizes.getFirst().product(),
                    sizes.stream().map(v -> new Size(v.variant(), v.quantity())).toList());
        }
    }

    @Schema(name = "ReportOption", description = "Uma opção ou adicional: quantas vezes e quanto somou")
    record Option(@Schema(requiredMode = REQUIRED) String group, @Schema(requiredMode = REQUIRED) String name,
                  @Schema(requiredMode = REQUIRED) int times, @Schema(requiredMode = REQUIRED) long chargedCents,
                  @Schema(requiredMode = REQUIRED, description = "Tem preço (mesmo que tenha saído de graça)")
                  boolean priced) {
        static Option of(OptionSales o) {
            return new Option(o.group(), o.option(), o.times(), o.charged().cents(), o.priced());
        }
    }

    @Schema(name = "ReportPair", description = "Dos que pediram o primeiro, quantos pediram também o segundo")
    record Pair(@Schema(requiredMode = REQUIRED) String first, @Schema(requiredMode = REQUIRED) String second,
                @Schema(requiredMode = REQUIRED) int firstTabs, @Schema(requiredMode = REQUIRED) int together) {
        static Pair of(ProductPair p) {
            return new Pair(p.first(), p.second(), p.firstTabs(), p.together());
        }
    }

    @Schema(name = "ReportVideo", description = "Os produtos ativos com vídeo e sem, e o que cada grupo vendeu")
    record Video(@Schema(requiredMode = REQUIRED) int withVideo, @Schema(requiredMode = REQUIRED) int withVideoSold,
                 @Schema(requiredMode = REQUIRED) int withoutVideo,
                 @Schema(requiredMode = REQUIRED) int withoutVideoSold) {
        static Video of(VideoComparison v) {
            return new Video(v.withVideo(), v.withVideoSold(), v.withoutVideo(), v.withoutVideoSold());
        }
    }

    @Schema(name = "ReportProductInterest", description = "Quantos celulares viram o produto e quantos desses pediram")
    record Interest(@Schema(requiredMode = REQUIRED) String name, @Schema(requiredMode = REQUIRED) int viewers,
                    @Schema(requiredMode = REQUIRED) int ordered) {
        static Interest of(ProductInterest p) {
            return new Interest(p.name(), p.viewers(), p.ordered());
        }
    }

    @Schema(name = "ReportLeftInCart", description = "Um produto que ficou no carrinho: em quantos celulares e unidades")
    record Left(@Schema(requiredMode = REQUIRED) String name, @Schema(requiredMode = REQUIRED) int sessions,
                @Schema(requiredMode = REQUIRED) int units) {
        static Left of(LeftInCart l) {
            return new Left(l.name(), l.sessions(), l.units());
        }
    }

    @Schema(name = "ReportMenuInterest", description = """
            Os eventos do celular (REL-27, 28, 29). "Visto": abriu o prato ou viu o reel 2 s; "pediu": o mesmo
            celular pediu o produto.""")
    record PhoneInterest(
            @Schema(description = "O dia do primeiro evento gravado, no fuso do restaurante; ausente sem nenhum")
            LocalDate recordingSince,
            @Schema(requiredMode = REQUIRED, description = "Visto x pedido, dos mais vistos") List<Interest> viewed,
            @Schema(requiredMode = REQUIRED, description = "Os reels mais vistos") List<Interest> reels,
            @Schema(requiredMode = REQUIRED, description = "Celulares que puseram algo no carrinho")
            int cartSessions,
            @Schema(requiredMode = REQUIRED, description = "Desses, os que não enviaram pedido nenhum")
            int cartsNotSent,
            @Schema(requiredMode = REQUIRED, description = "O que ficou no carrinho sem ser pedido, pelo preço de hoje")
            long leftCents,
            @Schema(requiredMode = REQUIRED, description = "Os produtos que mais ficam no carrinho")
            List<Left> mostLeft) {
        static PhoneInterest of(MenuInterest i, ZoneId zone) {
            return new PhoneInterest(
                    i.recordingSince() == null ? null : i.recordingSince().atZone(zone).toLocalDate(),
                    i.viewed().stream().map(Interest::of).toList(), i.reels().stream().map(Interest::of).toList(),
                    i.carts().withCart(), i.carts().notSent(), i.leftValue().cents(),
                    i.mostLeft().stream().map(Left::of).toList());
        }
    }

    @Schema(name = "MenuFull", description = "Os blocos do Business")
    record Full(@Schema(requiredMode = REQUIRED) List<Canceled> mostCanceled,
                @Schema(requiredMode = REQUIRED) List<ProductSizes> sizes,
                @Schema(requiredMode = REQUIRED) List<Option> options,
                @Schema(requiredMode = REQUIRED, description = "O que os adicionais pagos somaram")
                long paidOptionsCents,
                @Schema(requiredMode = REQUIRED) List<Pair> pairs,
                @Schema(requiredMode = REQUIRED) Video video,
                @Schema(requiredMode = REQUIRED) PhoneInterest interest) {
        static Full of(MenuFull f, ZoneId zone) {
            return new Full(f.mostCanceled().stream().map(Canceled::of).toList(),
                    f.sizes().stream().map(ProductSizes::of).toList(), f.options().stream().map(Option::of).toList(),
                    f.paidOptions().cents(), f.pairs().stream().map(Pair::of).toList(), Video.of(f.video()),
                    PhoneInterest.of(f.interest(), zone));
        }
    }
}
