package az.simplexs.simplexs.dto.personal;

public record PersonalRol(
        Long personalId,
        Long rolId,
        String rolAdi,
        String aciqlama,
        Boolean esasRol) {
}
