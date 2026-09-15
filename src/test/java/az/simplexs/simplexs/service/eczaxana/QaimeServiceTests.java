package az.simplexs.simplexs.service.eczaxana;

import az.simplexs.simplexs.repository.eczaxana.*;
import az.simplexs.simplexs.repository.eczaxana.QaimeRepository.Operation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class QaimeServiceTests {
    final QaimeRepository repo=mock(QaimeRepository.class);
    final AnbarKontekstRepository context=mock(AnbarKontekstRepository.class);
    final QaimeService service=new QaimeService(repo,context,new ObjectMapper());
    final QaimeService.Scope scope=new QaimeService.Scope(1L,24L,2L,2026);
    @BeforeEach void setup(){
        when(context.canAccess(1L,24L)).thenReturn(true);
        when(context.warehouses(1L,24L)).thenReturn(List.of(new AnbarKontekstRepository.Warehouse(2L,"Anbar",1L,"Əsas",LocalDate.of(2025,1,1),false,true,"FIFO","FIFO")));
        when(context.years(1L,2L)).thenReturn(List.of(new AnbarKontekstRepository.Year(2026,true,LocalDate.of(2025,1,1))));
        when(repo.invoice(1L,2L,2026,7L)).thenReturn(Optional.of(Map.of("qaime_id",7L)));
        when(repo.execute(any(),any())).thenReturn(Map.of("status_kodu","UGURLU","qaime_id",7L));
    }
    @Test void rejectsWarehouseAndYearTamperingBeforeReadingInvoices(){
        assertThatThrownBy(()->service.details(new QaimeService.Scope(1L,24L,99L,2026),7L)).hasMessage("denied");
        assertThatThrownBy(()->service.details(new QaimeService.Scope(1L,24L,2L,2024),7L)).hasMessage("invalidYear");
        verifyNoInteractions(repo);
    }
    @Test void rejectsMissingModulePermission(){
        when(context.canAccess(1L,24L)).thenReturn(false);
        assertThatThrownBy(()->service.details(scope,7L)).hasMessage("denied");verifyNoInteractions(repo);
    }
    @Test void keepsMaterialDetailsInsideSelectedInvoiceAndYear(){
        assertThatThrownBy(()->service.details(scope,8L)).hasMessage("notFound");
        verify(repo,never()).materials(any(),any(),any());
    }
    @Test void usesAuthenticatedScopeAndActorForCreate(){
        service.write(scope,Operation.CREATE,null,null,Map.of("qaime_tarixi","2026-09-15","materiallar",List.of(Map.of("material_id",5,"alis_qiymeti","0.25")),"klinika_id",999,"anbar_id",999,"yaradan_personal_id",999));
        ArgumentCaptor<Map<String,Object>> captor=ArgumentCaptor.captor();verify(repo).execute(eq(Operation.CREATE),captor.capture());
        assertThat(captor.getValue().get("klinika_id")).isEqualTo(1L);assertThat(captor.getValue().get("anbar_id")).isEqualTo(2L);
        assertThat(captor.getValue().get("yaradan_personal_id")).isEqualTo(24L);
        assertThat(captor.getValue().get("materiallar").toString()).contains("0.25");
    }
    @Test void rejectsCreateOutsideYearAndWithoutMaterials(){
        assertThatThrownBy(()->service.write(scope,Operation.CREATE,null,null,Map.of("qaime_tarixi","2025-09-15","materiallar",List.of(Map.of("material_id",1))))).hasMessage("invalidYear");
        assertThatThrownBy(()->service.write(scope,Operation.CREATE,null,null,Map.of("qaime_tarixi","2026-09-15","materiallar",List.of()))).hasMessage("materialsRequired");
        verify(repo,never()).execute(any(),any());
    }
    @Test void rechecksMaterialLocksAndMembershipOnWrites(){
        when(repo.materials(1L,2L,7L)).thenReturn(List.of(Map.of("qaime_material_id",3L,"deyisdirile_biler",false,"siline_biler",false)));
        assertThatThrownBy(()->service.write(scope,Operation.DELETE_MATERIAL,7L,3L,Map.of())).hasMessage("materialLocked");
        assertThatThrownBy(()->service.write(scope,Operation.UPDATE_MATERIAL,7L,3L,Map.of())).hasMessage("materialLocked");
        assertThatThrownBy(()->service.write(scope,Operation.DELETE_MATERIAL,7L,99L,Map.of())).hasMessage("notFound");
        verify(repo,never()).execute(any(),any());
    }
    @Test void databaseBusinessFailureRaisesExceptionForTransactionRollback(){
        when(repo.execute(any(),any())).thenReturn(Map.of("status_kodu","QAIME_AYI_KILIDLIDIR"));
        assertThatThrownBy(()->service.write(scope,Operation.UPDATE,7L,null,Map.of())).hasMessage("QAIME_AYI_KILIDLIDIR");
    }
    @Test void computesTotalsUsingDecimals(){
        when(repo.materials(1L,2L,7L)).thenReturn(List.of(Map.of("alis_meblegi",new BigDecimal("0.10"),"satis_meblegi",new BigDecimal("0.20")),Map.of("alis_meblegi",new BigDecimal("0.20"),"satis_meblegi",new BigDecimal("0.30"))));
        var detail=service.details(scope,7L);assertThat(detail.get("purchaseTotal")).isEqualTo(new BigDecimal("0.30"));assertThat(detail.get("saleTotal")).isEqualTo(new BigDecimal("0.50"));
    }
}
