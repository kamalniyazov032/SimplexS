package az.simplexs.simplexs.controller;

import az.simplexs.simplexs.dto.modul.ModulListItem;
import az.simplexs.simplexs.dto.modul.ModulSistemi;
import az.simplexs.simplexs.dto.klinika.KlinikaListItem;
import az.simplexs.simplexs.repository.modul.ModulRepository;
import az.simplexs.simplexs.repository.klinika.KlinikaRepository;
import az.simplexs.simplexs.repository.tercume.TercumeRepository;
import az.simplexs.simplexs.repository.xeta.XetaJurnaliRepository;
import az.simplexs.simplexs.security.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import javax.sql.DataSource;
import java.util.List;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties={"spring.flyway.enabled=false"})
@AutoConfigureMockMvc
class ModulPageTests {
    @Autowired MockMvc mvc;
    @MockitoBean DataSource dataSource;
    @MockitoBean ModulRepository modules;
    @MockitoBean KlinikaRepository clinics;
    @MockitoBean TercumeRepository translations;
    @MockitoBean XetaJurnaliRepository journal;
    @MockitoBean AccessService access;

    @Test void rendersFourthLevelAndNestedParentChoices() throws Exception {
        var session = new MockHttpSession();
        var personal = new AuthenticatedPersonal(24L,"admin","","Test",true,List.of());
        session.setAttribute("SPRING_SECURITY_CONTEXT",new SecurityContextImpl(
                UsernamePasswordAuthenticationToken.authenticated(personal,null,List.of())));
        session.setAttribute(KlinikaController.SELECTED_KLINIKA_ID,1L);
        when(access.hasFullAccess(any())).thenReturn(true);
        when(access.clinicIds(any())).thenReturn(List.of(1L));
        var clinic = mock(KlinikaListItem.class);
        when(clinic.klinikaId()).thenReturn(1L); when(clinic.aktiv()).thenReturn(true);
        when(clinic.klinikaAdi()).thenReturn("Klinika"); when(clinics.findAll()).thenReturn(List.of(clinic));
        when(modules.findSystems()).thenReturn(List.of(new ModulSistemi(1L,"HIS","Klinika",null,1,true)));
        when(modules.findAll()).thenReturn(List.of(
                node(1,null,0,"Əczaxana","Əczaxana"),
                node(2,1L,1,"Anbar tələbləri","Əczaxana / Anbar tələbləri"),
                node(3,2L,2,"Göndərdiklərim","Əczaxana / Anbar tələbləri / Göndərdiklərim")));
        var pharmacy = new MenuModule(1L,null,"P","Əczaxana",null,null);
        var requests = new MenuModule(2L,1L,"R","Anbar tələbləri",null,null);
        requests.getChildren().add(new MenuModule(3L,2L,"S","Göndərdiklərim","/sent",null));
        pharmacy.getChildren().add(requests);
        var system = new MenuSystem(1L,"HIS","Klinika",null); system.getModules().add(pharmacy);
        when(access.menuSystems(any(),any())).thenReturn(List.of(system));
        var html=mvc.perform(get("/modullar").session(session)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(html).contains("Səviyyə 4","Əczaxana / Anbar tələbləri","module-parent-select","href=\"/sent\"");
        assertThat(html).doesNotContain("modules.hierarchy_four", "modules.parent_label");
        verifyNoInteractions(dataSource);
    }
    private ModulListItem node(long id,Long parent,int depth,String name,String path) {
        return new ModulListItem(1L,"HIS","Klinika",null,id,parent,"M"+id,name,null,null,null,true,true,1,depth,path);
    }
}
