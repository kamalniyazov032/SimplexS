package az.simplexs.simplexs.dto.rol;

public record Rol(Long rolId, Long klinikaId, String rolAdi, String aciqlama, Boolean sistemRoludur,
                  Integer siraNo, Boolean aktiv, java.time.LocalDateTime yaranmaTarixi) {
}
