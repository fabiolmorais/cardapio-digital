package br.com.cardapio.application.service.menu;

import br.com.cardapio.application.port.out.media.ImageProcessorPort;
import br.com.cardapio.application.port.out.media.MediaStoragePort;
import br.com.cardapio.application.port.out.media.ProcessedImage;
import br.com.cardapio.application.port.out.media.ProcessedLogo;
import br.com.cardapio.application.port.out.media.SignedUpload;
import br.com.cardapio.application.port.out.menu.ProductMediaPort;
import br.com.cardapio.application.port.out.video.HostedVideo;
import br.com.cardapio.application.port.out.video.VideoHostingPort;
import br.com.cardapio.application.port.out.video.VideoUploadTicket;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.menu.model.ImageVariant;
import br.com.cardapio.domain.menu.model.MediaStatus;
import br.com.cardapio.domain.menu.model.ProductId;
import br.com.cardapio.domain.menu.model.ProductMedia;
import br.com.cardapio.domain.menu.model.ProductMediaId;
import br.com.cardapio.domain.shared.exception.InvalidImageException;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Mídias, armazenamento, processador e serviço de vídeo em memória. O "processador" aceita
 * qualquer arquivo que comece com "FOTO" e recusa o resto: o processamento de verdade tem o
 * próprio teste. O "serviço de vídeo" deixa cada vídeo em conversão até o teste dizer como acabou.
 */
final class MediaFakes implements ProductMediaPort, MediaStoragePort, ImageProcessorPort, VideoHostingPort {

    final Map<ProductMediaId, ProductMedia> media = new LinkedHashMap<>();
    final Map<String, byte[]> files = new LinkedHashMap<>();
    final Map<String, HostedVideo> videos = new LinkedHashMap<>();

    /** O serviço de vídeo terminou de converter. */
    void videoConverted(String videoId, int width, int height, int seconds) {
        videos.put(videoId, new HostedVideo(HostedVideo.Status.READY, width, height, seconds));
    }

    void videoFailed(String videoId) {
        videos.put(videoId, new HostedVideo(HostedVideo.Status.FAILED, 0, 0, 0));
    }

    /** Faz o papel do navegador: envia o arquivo para a URL do passo 1. */
    void browserUploads(String url, byte[] content) {
        files.put(url.replace("https://r2.test/", ""), content);
    }

    // ProductMediaPort
    @Override
    public void save(ProductMedia m) { media.put(m.id(), m); }

    @Override
    public Optional<ProductMedia> find(EstablishmentId establishmentId, ProductMediaId id) {
        return Optional.ofNullable(media.get(id)).filter(m -> m.establishmentId().equals(establishmentId));
    }

    @Override
    public List<ProductMedia> findActiveByProduct(EstablishmentId establishmentId, ProductId productId) {
        return media.values().stream()
                .filter(m -> m.establishmentId().equals(establishmentId) && m.productId().equals(productId)
                        && m.active())
                .sorted(Comparator.comparingInt(ProductMedia::position))
                .toList();
    }

    @Override
    public int nextPosition(EstablishmentId establishmentId, ProductId productId) {
        return media.values().stream().filter(m -> m.productId().equals(productId))
                .mapToInt(m -> m.position() + 1).max().orElse(0);
    }

    @Override
    public List<ProductMedia> findReadyGalleries(EstablishmentId establishmentId) {
        return media.values().stream()
                .filter(m -> m.establishmentId().equals(establishmentId) && m.active()
                        && m.status() == MediaStatus.READY)
                .sorted(Comparator.comparing((ProductMedia m) -> m.productId().value())
                        .thenComparingInt(ProductMedia::position))
                .toList();
    }

    @Override
    public Optional<ProductMedia> findByVideoId(String videoId) {
        return media.values().stream().filter(m -> videoId.equals(m.videoId())).findFirst();
    }

    // MediaStoragePort
    @Override
    public SignedUpload signUpload(String path, String contentType, long sizeBytes, Duration validity) {
        return new SignedUpload("https://r2.test/" + path, Map.of("Content-Type", contentType),
                Instant.parse("2026-10-30T12:05:00Z"));
    }

    @Override
    public Optional<byte[]> read(String path, long maxBytes) {
        return Optional.ofNullable(files.get(path)).filter(f -> f.length <= maxBytes);
    }

    @Override
    public void store(String path, byte[] content, String contentType) { files.put(path, content); }

    @Override
    public void delete(String path) { files.remove(path); }

    @Override
    public String publicUrl(String path) { return "https://fotos.test/" + path; }

    // ImageProcessorPort
    @Override
    public ProcessedImage process(byte[] original) {
        if (!new String(original).startsWith("FOTO")) {
            throw new InvalidImageException("O arquivo não é uma foto JPG ou PNG");
        }
        Map<ImageVariant, byte[]> variants = Arrays.stream(ImageVariant.values())
                .collect(Collectors.toMap(Function.identity(), v -> v.name().getBytes()));
        return new ProcessedImage(variants, 1440, 960);
    }

    /** As fotos não usam; está aqui porque o processador é um só (a logo tem o próprio teste). */
    @Override
    public ProcessedLogo processLogo(byte[] original) {
        throw new UnsupportedOperationException();
    }

    // VideoHostingPort
    @Override
    public String createVideo(String title) {
        String id = "video-" + (videos.size() + 1);
        videos.put(id, new HostedVideo(HostedVideo.Status.PROCESSING, 0, 0, 0));
        return id;
    }

    @Override
    public VideoUploadTicket signUpload(String videoId, Duration validity) {
        return new VideoUploadTicket("https://video.test/tus", Map.of("VideoId", videoId),
                Instant.parse("2026-10-30T14:00:00Z"));
    }

    @Override
    public Optional<HostedVideo> find(String videoId) {
        return Optional.ofNullable(videos.get(videoId));
    }

    @Override
    public void deleteVideo(String videoId) { videos.remove(videoId); }

    @Override
    public String playbackUrl(String videoId) { return "https://video.test/" + videoId + ".mp4"; }

    @Override
    public String posterUrl(String videoId) { return "https://video.test/" + videoId + ".jpg"; }
}
