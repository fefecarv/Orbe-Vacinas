package br.com.orbe.model;
import java.time.*;
public class BloqueioAgenda {
 private Long id; private String unidade,motivo; private LocalDate dataBloqueio; private LocalTime horaInicio,horaFim; private LocalDateTime criadoEm;
 public Long getId(){return id;} public void setId(Long v){id=v;} public String getUnidade(){return unidade;} public void setUnidade(String v){unidade=v;} public String getMotivo(){return motivo;} public void setMotivo(String v){motivo=v;} public LocalDate getDataBloqueio(){return dataBloqueio;} public void setDataBloqueio(LocalDate v){dataBloqueio=v;} public LocalTime getHoraInicio(){return horaInicio;} public void setHoraInicio(LocalTime v){horaInicio=v;} public LocalTime getHoraFim(){return horaFim;} public void setHoraFim(LocalTime v){horaFim=v;} public LocalDateTime getCriadoEm(){return criadoEm;} public void setCriadoEm(LocalDateTime v){criadoEm=v;}
}
