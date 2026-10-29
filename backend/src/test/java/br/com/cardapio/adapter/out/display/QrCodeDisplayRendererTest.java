package br.com.cardapio.adapter.out.display;

import br.com.cardapio.application.port.out.floor.DisplayContent;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static br.com.cardapio.support.PrintedFiles.pdfPages;
import static br.com.cardapio.support.PrintedFiles.pdfText;
import static br.com.cardapio.support.PrintedFiles.readQr;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Teste do adaptador sem Spring: gera os arquivos e os lê de volta. */
class QrCodeDisplayRendererTest {

    private static final String URL = "https://app.cardapio.com.br/m/Qm9hdGVyYXRlbnRhcmFkaXZpbmhhcmVzdGV0b2tlbg";

    private final QrCodeDisplayRenderer renderer = new QrCodeDisplayRenderer();

    @Test
    void pngIsAQrCodeThatOpensTheUrl() throws Exception {
        byte[] png = renderer.qrPng(URL);

        assertThat(png).startsWith(0x89, 'P', 'N', 'G');
        assertThat(readQr(png)).isEqualTo(URL);
    }

    @Test
    void svgIsAVectorWithTheSameModules() {
        String svg = new String(renderer.qrSvg(URL), StandardCharsets.UTF_8);

        int modules = renderer.encode(URL).getWidth();
        assertThat(svg).startsWith("<svg xmlns=\"http://www.w3.org/2000/svg\"")
                .contains("viewBox=\"0 0 " + modules + " " + modules + "\"")
                .contains("<path d=\"M");
    }

    @Test
    void pdfHasOnePagePerTableWithTheTexts() throws Exception {
        byte[] pdf = renderer.displaysPdf(List.of(
                new DisplayContent("Sakura Sushi", "04", URL),
                new DisplayContent("Sakura Sushi", "Varanda 2", URL)));

        assertThat(pdf).startsWith('%', 'P', 'D', 'F');
        assertThat(pdfPages(pdf)).isEqualTo(2);
        assertThat(pdfText(pdf, 1)).contains("SAKURA SUSHI", "Mesa 04", "Aponte a câmera");
        assertThat(pdfText(pdf, 2)).contains("Mesa Varanda 2");
    }

    @Test
    void pdfNeedsAtLeastOneDisplay() {
        assertThatThrownBy(() -> renderer.displaysPdf(List.of())).isInstanceOf(IllegalArgumentException.class);
    }
}
