package az.simplexs.simplexs.security;

import az.simplexs.simplexs.dto.modul.ModulListItem;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** System heading is level one; modules occupy levels two through four. */
public final class ModuleHierarchy {
    private ModuleHierarchy() {}

    public static List<ModulListItem> parents(List<ModulListItem> modules, Long movingId) {
        Set<Long> subtree = new HashSet<>();
        if (movingId != null) subtree.add(movingId);
        boolean changed;
        do {
            changed = false;
            for (var module : modules) {
                if (subtree.contains(module.parentId())) changed |= subtree.add(module.id());
            }
        } while (changed);
        int rootDepth = modules.stream().filter(m -> m.id().equals(movingId))
                .mapToInt(m -> m.seviyye()).findFirst().orElse(0);
        int height = modules.stream().filter(m -> subtree.contains(m.id()))
                .mapToInt(m -> m.seviyye() - rootDepth).max().orElse(0);
        return modules.stream().filter(m -> !subtree.contains(m.id()))
                .filter(m -> m.seviyye() + 1 + height <= 2)
                .filter(m -> m.route() == null || m.route().isBlank()).toList();
    }
}
