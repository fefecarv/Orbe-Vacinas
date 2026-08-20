package br.com.orbe.model;
import java.time.LocalTime;
public class ConfiguracaoAgenda {
 private Long id; private String unidade; private int diaSemana; private LocalTime horaAbertura,horaFechamento; private int intervaloMinutos; private boolean ativo;
 public Long getId(){return id;} public void setId(Long v){id=v;} public String getUnidade(){return unidade;} public void setUnidade(String v){unidade=v;} public int getDiaSemana(){return diaSemana;} public void setDiaSemana(int v){diaSemana=v;} public LocalTime getHoraAbertura(){return horaAbertura;} public void setHoraAbertura(LocalTime v){horaAbertura=v;} public LocalTime getHoraFechamento(){return horaFechamento;} public void setHoraFechamento(LocalTime v){horaFechamento=v;} public int getIntervaloMinutos(){return intervaloMinutos;} public void setIntervaloMinutos(int v){intervaloMinutos=v;} public boolean isAtivo(){return ativo;} public void setAtivo(boolean v){ativo=v;}
}
