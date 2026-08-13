package br.com.orbe.service.impl;

import br.com.orbe.dao.LoteDao;
import br.com.orbe.dao.VacinaDao;
import br.com.orbe.exception.BusinessException;
import br.com.orbe.model.Lote;
import br.com.orbe.model.Vacina;
import br.com.orbe.service.CatalogoService;

import java.time.LocalDate;
import java.util.List;

public class CatalogoServiceImpl implements CatalogoService {

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
        if (vacina.getNome() == null || vacina.getNome().isBlank()) {
            throw new BusinessException("O nome da vacina é obrigatório.");
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
