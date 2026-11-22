package br.com.cardapio.adapter.out.persistence.device;

import br.com.cardapio.application.port.out.device.StaffShiftPort;
import br.com.cardapio.domain.account.model.EstablishmentId;
import br.com.cardapio.domain.account.model.RefreshTokenId;
import br.com.cardapio.domain.account.model.UserId;
import br.com.cardapio.domain.device.model.DeviceId;
import br.com.cardapio.domain.device.model.ShiftEnd;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/** Os turnos de quem entra pelo PIN, em SQL parametrizado. */
@Component
class StaffShiftPersistenceAdapter implements StaffShiftPort {

    private final JdbcClient jdbc;

    StaffShiftPersistenceAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    private static OffsetDateTime at(Instant instant) {
        return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    @Override
    public void start(EstablishmentId establishmentId, UserId userId, DeviceId deviceId, RefreshTokenId familyId,
                      Instant now) {
        endDevice(deviceId, ShiftEnd.REPLACED, now);
        jdbc.sql("""
                        INSERT INTO staff_shift (id, establishment_id, user_id, device_id, family_id, started_at)
                        VALUES (:id, :establishmentId, :userId, :deviceId, :familyId, :now)
                        """)
                .param("id", UUID.randomUUID())
                .param("establishmentId", establishmentId.value())
                .param("userId", userId.value())
                .param("deviceId", deviceId.value())
                .param("familyId", familyId.value())
                .param("now", at(now))
                .update();
    }

    @Override
    public void endSession(RefreshTokenId familyId, ShiftEnd reason, Instant now) {
        jdbc.sql("""
                        UPDATE staff_shift SET ended_at = greatest(started_at, :now), end_reason = :reason
                        WHERE family_id = :familyId AND ended_at IS NULL
                        """)
                .param("familyId", familyId.value())
                .param("reason", reason.name())
                .param("now", at(now))
                .update();
    }

    @Override
    public void endDevice(DeviceId deviceId, ShiftEnd reason, Instant now) {
        jdbc.sql("""
                        UPDATE staff_shift SET ended_at = greatest(started_at, :now), end_reason = :reason
                        WHERE device_id = :deviceId AND ended_at IS NULL
                        """)
                .param("deviceId", deviceId.value())
                .param("reason", reason.name())
                .param("now", at(now))
                .update();
    }

    @Override
    public int endExpired(Instant now) {
        // A sessão vale enquanto algum refresh token dela não venceu nem foi revogado.
        return jdbc.sql("""
                        UPDATE staff_shift s
                        SET end_reason = 'EXPIRED',
                            ended_at = greatest(s.started_at,
                                (SELECT max(coalesce(r.revoked_at, r.created_at)) FROM refresh_token r
                                 WHERE r.family_id = s.family_id))
                        WHERE s.ended_at IS NULL
                          AND NOT EXISTS (SELECT 1 FROM refresh_token r
                                          WHERE r.family_id = s.family_id
                                            AND r.revoked_at IS NULL AND r.expires_at > :now)
                        """)
                .param("now", at(now))
                .update();
    }
}
