package br.com.orbe.service.impl;

import br.com.orbe.dao.LoteDao;
import br.com.orbe.dao.VacinaDao;
import br.com.orbe.exception.BusinessException;
import br.com.orbe.model.Lote;
import br.com.orbe.model.Vacina;
import br.com.orbe.service.CatalogoService;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class CatalogoServiceImpl implements CatalogoService {

    private static final Set<String> CATEGORIAS_PERMITIDAS = Set.of("INFANTIL", "ADULTO");

    private final VacinaDao vacinaDao;
    private final LoteDao loteDao;

    public CatalogoServiceImpl(VacinaDao vacinaDao, LoteDao loteDao) {
        this.vacinaDao = vacinaDao;
        this.loteDao = loteDao;
    }

    @Override
    public List<Vacina> listarVacinasDisponiveis() {
        return vacinaDao.listarDisponiveis();
    }

    @Override
    public Vacina salvarVacina(Vacina vacina) {
        if (vacina == null) {
            throw new BusinessException("Os dados da vacina são obrigatórios.");
        }
        if (vacina.getNome() == null || vacina.getNome().isBlank()) {
            throw new BusinessException("O nome da vacina é obrigatório.");
        }
        if (vacina.getDescricao() == null || vacina.getDescricao().isBlank()) {
            throw new BusinessException("A descrição da vacina é obrigatória.");
        }
        if (vacina.getCategoria() == null || vacina.getCategoria().isBlank()
                || vacina.getFabricante() == null || vacina.getFabricante().isBlank()) {
            throw new BusinessException("Fabricante e categoria são obrigatórios.");
        }
        String categoriaNormalizada = vacina.getCategoria().trim().toUpperCase(Locale.ROOT);
        if (!CATEGORIAS_PERMITIDAS.contains(categoriaNormalizada)) {
            throw new BusinessException("A categoria deve ser Infantil ou Adulto.");
        }
        vacina.setCategoria(categoriaNormalizada.equals("INFANTIL") ? "Infantil" : "Adulto");
        if (vacina.getIdadeMinimaMeses() < 0) {
            throw new BusinessException("A idade mínima não pode ser negativa.");
        }
        if (vacina.getIdadeMaximaMeses() != null
                && vacina.getIdadeMaximaMeses() < vacina.getIdadeMinimaMeses()) {
            throw new BusinessException("A idade máxima deve ser igual ou maior que a idade mínima.");
        }
        if (vacina.getNumeroDoses() < 1) {
            throw new BusinessException("A quantidade de doses deve ser igual ou maior que 1.");
        }
        if (vacina.getNumeroDoses() > 1
                && (vacina.getIntervaloDias() == null || vacina.getIntervaloDias() < 1)) {
            throw new BusinessException("Informe o intervalo entre doses para esquemas com mais de uma dose.");
        }
        if (vacina.getIntervaloDias() != null && vacina.getIntervaloDias() < 1) {
            throw new BusinessException("O intervalo entre doses deve ser maior que zero.");
        }
        if (vacina.getReforcoMeses() != null && vacina.getReforcoMeses() < 1) {
            throw new BusinessException("O período de reforço deve ser maior que zero.");
        }
        if (vacina.getValorBase() == null || vacina.getValorBase().signum() < 0) {
            throw new BusinessException("O valor-base não pode ser negativo.");
        }
        if (vacina.getId() == null) {
            vacina.setAtivo(true);
        }
        return vacina.getId() == null
                ? vacinaDao.salvar(vacina)
                : vacinaDao.atualizar(vacina);
    }

    @Override
    public Lote salvarLote(Lote lote) {
        if (lote.getDataValidade() == null || lote.getDataValidade().isBefore(LocalDate.now())) {
            throw new BusinessException("O lote deve possuir uma validade futura.");
        }
        if (lote.getQuantidadeInicial() <= 0) {
            throw new BusinessException("A quantidade inicial deve ser maior que zero.");
        }
        return lote.getId() == null
                ? loteDao.salvar(lote)
                : loteDao.atualizar(lote);
    }

    @Override
    public List<Lote> listarLotes(Long vacinaId) {
        return loteDao.listarValidosPorVacina(vacinaId);
    }
}
