package az.simplexs.simplexs.dto.ambulator;

import java.util.List;

public record AmbulatorLookups(
    List<LookupOption> buildings,
    List<LookupOption> organizations,
    List<LookupOption> idTypes,
    List<LookupOption> genders,
    List<LookupOption> countries,
    List<LookupOption> cities,
    List<LookupOption> bloodGroups,
    List<LookupOption> maritalStatuses,
    List<LookupOption> nationalities,
    List<LookupOption> educations
) {
}
