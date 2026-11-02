package br.com.cardapio.domain.presence.model;

import br.com.cardapio.domain.shared.exception.InvalidValueException;

/** O local do restaurante e o raio em volta dele, em metros. */
public record GeoFence(double latitude, double longitude, int radiusMeters) {

    public static final int MIN_RADIUS = 25;
    public static final int MAX_RADIUS = 1000;
    /** Precisão pior que esta não vale: dentro de um prédio, o GPS do celular costuma ficar entre 10 e 60 m. */
    public static final double MAX_ACCURACY_METERS = 100;
    private static final double EARTH_RADIUS_METERS = 6_371_000;

    public GeoFence {
        checkCoordinates(latitude, longitude);
        if (radiusMeters < MIN_RADIUS || radiusMeters > MAX_RADIUS) {
            throw new InvalidValueException("O raio deve ser de " + MIN_RADIUS + " a " + MAX_RADIUS + " metros");
        }
    }

    static void checkCoordinates(double latitude, double longitude) {
        if (!(latitude >= -90 && latitude <= 90) || !(longitude >= -180 && longitude <= 180)) {
            throw new InvalidValueException("Localização inválida");
        }
    }

    /**
     * A posição vale se a precisão for boa e o ponto cair dentro do raio, com uma folga do
     * tamanho da imprecisão: quem está na borda do salão com precisão de 40 m pode aparecer até
     * 40 m "fora" sem estar.
     */
    public boolean admits(GeoPosition position) {
        return position.accuracyMeters() <= MAX_ACCURACY_METERS
                && distanceTo(position.latitude(), position.longitude()) <= radiusMeters + position.accuracyMeters();
    }

    /** Distância em metros pela fórmula de haversine (a Terra como esfera: sobra precisão para 1 km). */
    public double distanceTo(double otherLatitude, double otherLongitude) {
        double dLat = Math.toRadians(otherLatitude - latitude);
        double dLon = Math.toRadians(otherLongitude - longitude);
        double a = Math.pow(Math.sin(dLat / 2), 2) + Math.cos(Math.toRadians(latitude))
                * Math.cos(Math.toRadians(otherLatitude)) * Math.pow(Math.sin(dLon / 2), 2);
        return 2 * EARTH_RADIUS_METERS * Math.asin(Math.sqrt(a));
    }
}
