package az.simplexs.simplexs.controller;

import az.simplexs.simplexs.dto.ambulator.Gelis;
import az.simplexs.simplexs.dto.klinika.KlinikaListItem;
import az.simplexs.simplexs.repository.ambulator.AmbulatorRepository;
import az.simplexs.simplexs.repository.klinika.KlinikaRepository;
import az.simplexs.simplexs.repository.tercume.TercumeRepository;
import az.simplexs.simplexs.repository.xestexidmet.XesteXidmetRepository;
import az.simplexs.simplexs.security.AccessService;
import az.simplexs.simplexs.security.AuthenticatedPersonal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"spring.flyway.enabled=false", "spring.datasource.hikari.read-only=true"})
@AutoConfigureMockMvc
class XesteXidmetPageTests {
    @Autowired MockMvc mvc;
    @MockitoBean AmbulatorRepository visits;
    @MockitoBean XesteXidmetRepository services;
    @MockitoBean KlinikaRepository clinics;
    @MockitoBean TercumeRepository translations;
    @MockitoBean AccessService access;
    private MockHttpSession session;

    @BeforeEach
    void prepareVisit() {
        var personal = new AuthenticatedPersonal(17L, "page-test", "", "Test", true, List.of());
        var authentication = UsernamePasswordAuthenticationToken.authenticated(personal, null, List.of());
        session = new MockHttpSession();
        session.setAttribute("SPRING_SECURITY_CONTEXT", new SecurityContextImpl(authentication));
        session.setAttribute(KlinikaController.SELECTED_KLINIKA_ID, 1L);
        when(access.hasFullAccess(any())).thenReturn(true);
        when(access.clinicIds(any())).thenReturn(List.of(1L));
        var clinic = mock(KlinikaListItem.class);
        when(clinic.klinikaId()).thenReturn(1L);
        when(clinic.aktiv()).thenReturn(true);
        when(clinic.klinikaAdi()).thenReturn("Test");
        when(clinics.findAll()).thenReturn(List.of(clinic));
        var visit = new Gelis(17L, 1L, "TEST001", "Test", "Pasiyent", "", null, null,
                null, 1L, "AMB", "Ambulator", null, null, null, null, "A00017",
                LocalDate.of(2026, 9, 6), LocalTime.NOON, false, null, null, null, null, true);
        when(visits.gelis(1L, 17L)).thenReturn(visit);
        when(services.gonderenHekim(17L)).thenReturn(Map.of());
    }

    @Test
    void addPageRendersDateAndLocalizedControls() throws Exception {
        mvc.perform(get("/xeste-xidmetleri/17").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"serviceDate\"")))
                .andExpect(content().string(containsString("Xidmət tarixi")))
                .andExpect(content().string(containsString("name=\"_csrf\"")));
    }

    @Test
    void allServicesPageRendersPatientFiltersAndExports() throws Exception {
        mvc.perform(get("/xeste-xidmetleri/17/hamisi").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("TEST001")))
                .andExpect(content().string(containsString("id=\"filter-status\"")))
                .andExpect(content().string(containsString("id=\"export-excel\"")));
    }

    @Test
    void editPageAndCatalogKeepRequestId() throws Exception {
        mvc.perform(get("/xeste-xidmetleri/17").param("istekId", "11").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-istek-id=\"11\"")));
        mvc.perform(get("/xeste-xidmetleri/17/kataloq").param("istekId", "11").session(session))
                .andExpect(status().isOk());
        verify(services).xidmetler(17L, null, null, 0, 11L, 1L);
    }
    @Test
    void brokenRequestingDoctorFunctionDoesNotCrashThePage() throws Exception {
        when(services.isteyenHekimler(17L)).thenThrow(
                new org.springframework.dao.InvalidDataAccessResourceUsageException("missing legacy column"));
        mvc.perform(get("/xeste-xidmetleri/17").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("bu həkim seçimi müvəqqəti bağlıdır")))
                .andExpect(content().string(containsString("id=\"serviceDate\"")));
    }

    @Test
    void doctorPricesUseTheSelectedServiceDate() throws Exception {
        mvc.perform(get("/xeste-xidmetleri/17/xidmet/7831/sobe/12/hekimler")
                        .param("tarix", "2026-09-05").session(session))
                .andExpect(status().isOk());
        verify(services).sobeHekimleri(17L, 7831L, 12L, LocalDate.of(2026, 9, 5));
    }

}
