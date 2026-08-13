package br.com.orbe.service;
import br.com.orbe.model.*;import br.com.orbe.dto.UsuarioAdministrativoItem;import br.com.orbe.dto.RelatorioGerencial;
import java.util.List;import java.time.LocalDate;
public interface AdministracaoService {
    List<UsuarioAdministrativoItem> listarUsuarios(); List<Lote> listarLotes(); Lote salvarLote(Lote lote);
    List<Convenio> listarConvenios(); Convenio salvarConvenio(Convenio convenio);
    List<MovimentacaoEstoque> listarMovimentacoes(); List<Auditoria> listarAuditoria();
    RelatorioGerencial relatorio(LocalDate inicio, LocalDate fim);
}
