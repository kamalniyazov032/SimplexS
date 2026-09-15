package az.simplexs.simplexs.security;

import az.simplexs.simplexs.dto.modul.ModulListItem;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class ModuleHierarchyTests {
    private ModulListItem node(long id, Long parent, int depth, String route) {
        return new ModulListItem(1L,"HIS","Klinika",null,id,parent,"M"+id,"M"+id,null,
                route,null,true,true,1,depth,"M"+id);
    }
    private final List<ModulListItem> tree = List.of(node(1,null,0,null),node(2,1L,1,null),
            node(3,2L,2,null),node(4,null,0,null),node(5,4L,1,"/page"));

    @Test void newNodesCanBeCreatedAtLevelsTwoThroughFour() {
        assertThat(ModuleHierarchy.parents(tree,null)).extracting(m -> m.id()).containsExactly(1L,2L,4L);
    }
    @Test void excludesSelfDescendantsAndParentsThatWouldMakeSubtreeTooDeep() {
        assertThat(ModuleHierarchy.parents(tree,1L)).isEmpty();
        assertThat(ModuleHierarchy.parents(tree,2L)).extracting(m -> m.id()).containsExactly(1L,4L);
        assertThat(ModuleHierarchy.parents(tree,3L)).extracting(m -> m.id()).containsExactly(1L,2L,4L);
    }
    @Test void currentPageExpandsAllAncestorsWithRouteBoundaries() {
        var pharmacy = new MenuModule(1L,null,"P","Əczaxana",null,null);
        var requests = new MenuModule(2L,1L,"R","Anbar tələbləri",null,null);
        var sent = new MenuModule(3L,2L,"S","Göndərdiklərim","/requests/sent",null);
        pharmacy.getChildren().add(requests); requests.getChildren().add(sent);
        assertThat(pharmacy.containsRoute("/requests/sent/42")).isTrue();
        assertThat(requests.containsRoute("/requests/sent/42")).isTrue();
        assertThat(pharmacy.containsRoute("/requests/sent-other")).isFalse();
    }
}
