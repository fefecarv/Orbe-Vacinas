package br.com.orbe.service;

import br.com.orbe.dto.UsuarioAutenticado;

public interface AutenticacaoService {

    UsuarioAutenticado autenticar(String email, String senha, String enderecoIp);
    void alterarSenha(Long usuarioId,String senhaAtual,String novaSenha);
}
