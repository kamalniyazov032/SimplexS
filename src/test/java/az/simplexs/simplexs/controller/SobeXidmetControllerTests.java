package az.simplexs.simplexs.controller;

import az.simplexs.simplexs.dto.sobe.SobeListItem;
import az.simplexs.simplexs.repository.sobe.SobeRepository;
import az.simplexs.simplexs.repository.xidmet.SobeXidmetRepository;
import az.simplexs.simplexs.repository.xidmet.XidmetRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SobeXidmetControllerTests {
    private final SobeRepository departments = mock(SobeRepository.class);
    private final XidmetRepository catalog = mock(XidmetRepository.class);
    private final SobeXidmetRepository assigned = mock(SobeXidmetRepository.class);
    private final SobeXidmetController controller = new SobeXidmetController(departments, catalog, assigned);

    private MockHttpSession session() {
        var session = new MockHttpSession();
        session.setAttribute(KlinikaController.SELECTED_KLINIKA_ID, 1L);
        var department = mock(SobeListItem.class);
        when(department.sobeId()).thenReturn(2L);
        when(department.aktiv()).thenReturn(true);
        when(departments.findByKlinikaId(1L)).thenReturn(List.of(department));
        return session;
    }

    @ParameterizedTest
    @ValueSource(strings = {"elave", "cixar"})
    void postPreservesBothFiltersAndPages(String operation) throws Exception {
        when(assigned.add(2L, List.of(10L))).thenReturn(Map.of("status_kodu", "1"));
        when(assigned.remove(2L, List.of(10L))).thenReturn(Map.of("status_kodu", "1"));
        var result = MockMvcBuilders.standaloneSetup(controller).build().perform(
                post("/parShobeXidmet/" + operation).session(session())
                        .param("sobeId", "2").param("xidmetIdleri", "10")
                        .param("qrupId", "3").param("q", "qan & sidik")
                        .param("page", "2").param("bagliQrupId", "4")
                        .param("bagliQ", "USM + analiz").param("bagliPage", "1"))
                .andExpect(status().is3xxRedirection()).andReturn();
        var params = UriComponentsBuilder.fromUriString(result.getResponse().getRedirectedUrl())
                .build().getQueryParams();
        assertEquals("3", params.getFirst("qrupId"));
        assertEquals("4", params.getFirst("bagliQrupId"));
        assertEquals("2", params.getFirst("page"));
        assertEquals("1", params.getFirst("bagliPage"));
        assertEquals("qan & sidik", java.net.URLDecoder.decode(params.getFirst("q"), java.nio.charset.StandardCharsets.UTF_8));
        assertEquals("USM + analiz", java.net.URLDecoder.decode(params.getFirst("bagliQ"), java.nio.charset.StandardCharsets.UTF_8));
        if (operation.equals("elave")) verify(assigned).add(2L, List.of(10L));
        else verify(assigned).remove(2L, List.of(10L));
    }

    @Test
    void assignedGroupFiltersCountAndRowsAndClampsPage() throws Exception {
        when(assigned.count(2L, 4L, "USM")).thenReturn(101L);
        MockMvcBuilders.standaloneSetup(controller).build().perform(
                get("/parShobeXidmet").session(session()).param("sobeId", "2")
                        .param("bagliQrupId", "4").param("bagliQ", "USM").param("bagliPage", "9"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("selectedBagliQrupId", 4L))
                .andExpect(model().attribute("bagliQ", "USM"))
                .andExpect(model().attribute("bagliPage", 1));
        verify(assigned).findPage(2L, 4L, "USM", 100, 100);
    }
}
