package az.simplexs.simplexs.dto.ambulator;

public record LookupOption(
    Integer id,
    String code,
    String name,
    String shortName,
    Integer countryId
) {
}
