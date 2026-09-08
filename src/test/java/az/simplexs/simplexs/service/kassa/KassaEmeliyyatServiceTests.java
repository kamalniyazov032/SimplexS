package az.simplexs.simplexs.service.kassa;

import az.simplexs.simplexs.repository.kassa.KassaEmeliyyatRepository;
import az.simplexs.simplexs.security.AccessService;
import az.simplexs.simplexs.security.AuthenticatedPersonal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.access.AccessDeniedException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class KassaEmeliyyatServiceTests {
    final KassaEmeliyyatRepository repo=mock(KassaEmeliyyatRepository.class);
    final AccessService access=mock(AccessService.class);
    final KassaEmeliyyatService service=new KassaEmeliyyatService(repo,access);
    final UsernamePasswordAuthenticationToken auth=UsernamePasswordAuthenticationToken.authenticated(
            new AuthenticatedPersonal(24L,"cashier","","Cashier",true,List.of()),null,List.of());
    @BeforeEach void setup() {
        when(access.hasClinic(auth,1L)).thenReturn(true);
        when(access.canAccessRoute(auth,1L,"/kassa")).thenReturn(true);
        when(repo.cashRegisters(1L,24L)).thenReturn(List.of(Map.of("kassa_id",7L,"izlesin",true,"islesin",true)));
        when(repo.lockPatient(eq(1L),eq(17L),anyBoolean())).thenReturn(true);
        when(repo.paymentTypes()).thenReturn(List.of(Map.of("odenis_novu_id",1L),Map.of("odenis_novu_id",2L)));
        when(repo.services(1L,7L,17L)).thenReturn(List.of(Map.of("xeste_xidmet_id",101L,"qalan_mebleg",new BigDecimal("12.30"))));
        when(repo.debtServices(1L,7L,17L)).thenReturn(List.of(Map.of("borclandirma_id",10L,"borclandirma_xidmet_id",201L,"qalan_borc",new BigDecimal("12.30"))));
    }
    private void pay(boolean debt,List<Long> ids,List<BigDecimal> amounts,boolean borrow) {
        service.pay(auth,1L,7L,17L,debt,ids,List.of(1L,2L),amounts,borrow," Test ");
    }
    @Test void paymentUsesAuthenticatedPersonalAndExactSplitAmounts() {
        pay(false,List.of(101L),List.of(new BigDecimal("5.10"),new BigDecimal("7.20")),false);
        verify(repo).pay(1L,7L,24L,17L,false,"[{\"xeste_xidmet_id\":101}]",
                "[{\"odenis_novu_id\":1,\"mebleg\":5.10},{\"odenis_novu_id\":2,\"mebleg\":7.20}]",false,"Test");
        var order=inOrder(repo);order.verify(repo).lockPatient(1L,17L,false);order.verify(repo).services(1L,7L,17L);
    }
    @Test void debtPaymentUsesDetailIdAndAllowsPartialRepayment() {
        pay(true,List.of(201L),List.of(new BigDecimal("5.00"),BigDecimal.ZERO),false);
        verify(repo).pay(eq(1L),eq(7L),eq(24L),eq(17L),eq(true),eq("[{\"borclandirma_xidmet_id\":201}]"),anyString(),eq(false),eq("Test"));
    }
    @Test void rejectsDebtHeaderId() {
        assertThatThrownBy(()->pay(true,List.of(10L),List.of(BigDecimal.ONE,BigDecimal.ZERO),false)).hasMessage("kassa.staleServices");
        verify(repo,never()).pay(any(),any(),any(),any(),anyBoolean(),any(),any(),anyBoolean(),any());
    }
    @Test void rejectsUnauthorizedClinicCashAndViewOnlyWrite() {
        when(access.hasClinic(auth,1L)).thenReturn(false);
        assertThatThrownBy(()->service.requireCash(auth,1L,7L,false)).isInstanceOf(AccessDeniedException.class);
        when(access.hasClinic(auth,1L)).thenReturn(true);
        assertThatThrownBy(()->service.requireCash(auth,1L,99L,false)).isInstanceOf(AccessDeniedException.class);
        when(repo.cashRegisters(1L,24L)).thenReturn(List.of(Map.of("kassa_id",7L,"izlesin",true,"islesin",false)));
        assertThat(service.requireCash(auth,1L,7L,false)).isNotEmpty();
        assertThatThrownBy(()->pay(false,List.of(101L),List.of(BigDecimal.ONE,BigDecimal.ZERO),false)).isInstanceOf(AccessDeniedException.class);
    }
    @Test void rejectsOverpaymentAndFractionalCents() {
        assertThatThrownBy(()->pay(false,List.of(101L),List.of(new BigDecimal("12.31"),BigDecimal.ZERO),false)).hasMessage("kassa.overpayment");
        assertThatThrownBy(()->pay(false,List.of(101L),List.of(new BigDecimal("12.301"),BigDecimal.ZERO),false)).hasMessage("kassa.invalidPayment");
    }
    @Test void requiresExplicitBorrowConsentAndPermission() {
        assertThatThrownBy(()->pay(false,List.of(101L),List.of(BigDecimal.ONE,BigDecimal.ZERO),false)).hasMessage("kassa.borrowRequired");
        assertThatThrownBy(()->pay(false,List.of(101L),List.of(BigDecimal.ONE,BigDecimal.ZERO),true)).isInstanceOf(AccessDeniedException.class);
        when(access.hasPermission(auth,1L,"SLX000004")).thenReturn(true);
        pay(false,List.of(101L),List.of(BigDecimal.ONE,BigDecimal.ZERO),true);
        verify(repo).pay(any(),any(),any(),any(),eq(false),anyString(),anyString(),eq(true),anyString());
    }
    @Test void rejectsDuplicateAndIneligibleServices() {
        assertThatThrownBy(()->pay(false,List.of(101L,101L),List.of(BigDecimal.ONE,BigDecimal.ZERO),false)).hasMessage("kassa.selectServices");
        when(repo.services(1L,7L,17L)).thenReturn(List.of(Map.of("xeste_xidmet_id",101L,"qalan_mebleg",BigDecimal.TEN,"paket_daxildir",true)));
        assertThatThrownBy(()->pay(false,List.of(101L),List.of(BigDecimal.TEN,BigDecimal.ZERO),false)).hasMessage("kassa.staleServices");
    }
    @Test void rejectsInactivePaymentMethodsAndNegativeAmounts() {
        when(repo.paymentTypes()).thenReturn(List.of());
        assertThatThrownBy(()->pay(false,List.of(101L),List.of(BigDecimal.ONE,BigDecimal.ZERO),false)).hasMessage("kassa.invalidPayment");
        assertThatThrownBy(()->pay(false,List.of(101L),List.of(new BigDecimal("-1"),BigDecimal.ZERO),false)).hasMessage("kassa.invalidPayment");
    }
}
