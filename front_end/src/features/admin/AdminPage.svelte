<script lang="ts">
  import { onMount } from 'svelte';
  import Button from '../../design-system/components/Button.svelte';
  import Card from '../../design-system/components/Card.svelte';
  import PageHeader from '../../design-system/components/PageHeader.svelte';
  import Toast from '../../design-system/components/Toast.svelte';
  import CollectionPanel from '../../design-system/components/CollectionPanel.svelte';
  import ViewModeToggle from '../../design-system/components/ViewModeToggle.svelte';
  import Tooltip from '../../design-system/components/Tooltip.svelte';
  import AdminCrudDialog from './AdminCrudDialog.svelte';
  import { movements } from '../../mocks/admin';
  import { adminApi, patientApi, type ManagementReport } from '../../lib/api';
  let {
    mode,
    onNavigate,
  }: {
    mode: 'dashboard' | 'users' | 'vaccines' | 'stock' | 'insurance' | 'reports' | 'audit';
    onNavigate: (page: string) => void;
  } = $props();
  let query = $state('');
  let page = $state(1);
  const pageSize = 4;
  const titles = {
    users: [
      'Acessos',
      'Usuários e perfis',
      'Cadastre pacientes, funcionários e administradores com permissões adequadas.',
    ],
    vaccines: ['Cadastros', 'Catálogo de vacinas', 'Mantenha as informações comerciais e clínicas das vacinas.'],
    stock: ['Estoque', 'Lotes e movimentações', 'Acompanhe quantidades, validades e o histórico do estoque.'],
    insurance: ['Cadastros', 'Convênios', 'Gerencie planos e códigos operacionais aceitos.'],
    reports: ['Indicadores', 'Relatórios', 'Analise a operação, aplicações e utilização do estoque.'],
    audit: ['Segurança', 'Auditoria', 'Consulte ações relevantes realizadas no sistema.'],
  } as const;
  const title = $derived(mode === 'dashboard' ? null : titles[mode]);
  let stockTab = $state<'batches' | 'movements'>('batches');
  type Entity = 'user' | 'vaccine' | 'batch' | 'insurance';
  type Row = Record<string, string>;
  function stored(key: string, fallback: Row[]) {
    try {
      return JSON.parse(localStorage.getItem(key) ?? 'null') ?? fallback;
    } catch {
      return fallback;
    }
  }
  let usersData = $state<Row[]>([]);
  let vaccinesData = $state<Row[]>([]);
  let batchesData = $state<Row[]>([]);
  let insuranceData = $state<Row[]>([]);
  let auditData = $state<Row[]>([]);
  let report = $state<ManagementReport>({pacientesAtivos:0,aplicacoesPeriodo:0,dosesEstoque:0,lotesAlerta:0,agendamentosTotal:0,concluidos:0,faltas:0,cancelados:0,dosesPerdidas:0,receita:0,aplicacoesPorSemana:[],vacinasMaisAplicadas:[],profissionaisMaisAtivos:[],alertas:[]});
  let reportDays = $state(30);
  let reportFormat = $state<'csv'|'print'>('csv');
  let auditAction = $state('TODAS');
  let auditPeriod = $state(30);
  let modalEntity = $state<Entity | null>(null);
  let editIndex = $state(-1);
  let toast = $state('');
  let advancedFilters = $state(false);
  let statusFilter = $state('Todos');
  let roleFilter = $state('Todos');
  let adminView = $state<'grid' | 'list'>((localStorage.getItem('orbe-view-admin') as 'grid' | 'list') ?? 'list');
  let filteredAudit = $derived(auditData.filter((item) => {
    const matchesQuery = Object.values(item).some((value) => value.toLowerCase().includes(query.toLowerCase()));
    const matchesAction = auditAction === 'TODAS' || item.actionCode === auditAction;
    const eventTime = new Date(item.rawDate).getTime();
    return matchesQuery && matchesAction && eventTime >= Date.now() - auditPeriod * 86_400_000;
  }));
  $effect(() => localStorage.setItem('orbe-view-admin', adminView));
  function currentRows(entity: Entity) {
    return entity === 'user'
      ? usersData
      : entity === 'vaccine'
        ? vaccinesData
        : entity === 'batch'
          ? batchesData
          : insuranceData;
  }
  function openCreate(entity: Entity) {
    modalEntity = entity;
    editIndex = -1;
  }
  function openEdit(index: number) {
    modalEntity =
      mode === 'users' ? 'user' : mode === 'vaccines' ? 'vaccine' : mode === 'stock' ? 'batch' : 'insurance';
    editIndex = index;
  }
  async function saveEntity(values: Row) {
    if (!modalEntity) return;
    try {
      const rows=currentRows(modalEntity);const id=editIndex>=0?Number(rows[editIndex].id):undefined;
      if(modalEntity==='vaccine')await adminApi.saveVaccine({id,nome:values.name,fabricante:values.manufacturer,descricao:values.description,categoria:values.category,indicacao:`A partir de ${values.ageMin} meses`,esquemaDoses:`${values.doseCount} dose(s)${values.intervalDays?` a cada ${values.intervalDays} dias`:''}`,valorBase:Number(values.price),ativo:id?values.status==='Ativo':true,idadeMinimaMeses:Number(values.ageMin),idadeMaximaMeses:values.ageMax?Number(values.ageMax):null,numeroDoses:Number(values.doseCount),intervaloDias:values.intervalDays?Number(values.intervalDays):null,reforcoMeses:values.boosterMonths?Number(values.boosterMonths):null});
      else if(modalEntity==='batch'){const vaccine=vaccinesData.find(item=>item.name.toLowerCase()===values.vaccine.toLowerCase());if(!vaccine)throw new Error('Selecione uma vacina cadastrada pelo nome exato.');await adminApi.saveLot({id,vacinaId:Number(vaccine.id),numeroLote:values.number,dataValidade:values.expires,quantidadeInicial:Number(values.quantity),quantidadeAtual:editIndex>=0?Number(rows[editIndex].quantity):Number(values.quantity),fornecedor:values.supplier,status:values.status==='Inativo'?'BLOQUEADO':'ATIVO'});}
      else if(modalEntity==='insurance')await adminApi.saveInsurance({id,nome:values.company,plano:values.plan,codigoOperacional:values.code,ativo:values.status==='Ativo',tipoCobertura:values.coverageType,percentualDesconto:values.discount?Number(values.discount):null,valorCoparticipacao:values.copay?Number(values.copay):null});
      else if(modalEntity==='user'){
        if(id)throw new Error('A alteração de perfil existente exige uma operação específica de segurança.');
        const profiles:Record<string,string>={Paciente:'PACIENTE',Funcionário:'FUNCIONARIO',Administrador:'ADMINISTRADOR'};
        await adminApi.createUser({nome:values.name,cpf:values.cpf,email:values.email,telefone:values.phone,dataNascimento:values.birth,senha:values.password,perfil:profiles[values.role],matricula:values.registration,unidade:values.unit,trocaSenhaObrigatoria:values.requirePasswordChange==='Sim'});
      }
      await loadAdminData();toast=editIndex>=0?'Registro atualizado com sucesso.':'Registro cadastrado com sucesso.';modalEntity=null;editIndex=-1;
    } catch(exception){toast=exception instanceof Error?exception.message:'Não foi possível salvar o registro.';}
  }
  function reportRange() {
    return { end: new Date().toISOString().slice(0,10), start: new Date(Date.now()-(reportDays-1)*86_400_000).toISOString().slice(0,10) };
  }
  async function loadReport() {
    const range = reportRange();
    report = await adminApi.report(range.start, range.end);
  }
  function humanAction(action:string, entity:string) {
    const actions:Record<string,string>={CRIAR:'Registrou',ATUALIZAR:'Atualizou',EXCLUIR:'Removeu',CANCELAR:'Cancelou',REAGENDAR:'Reagendou',LOGIN:'Entrou no sistema'};
    const entities:Record<string,string>={APLICACAO:'uma aplicação de vacina',AGENDAMENTO:'um agendamento',USUARIO:'um usuário',LOTE:'um lote de vacinas',VACINA:'uma vacina',CONVENIO:'um convênio'};
    return `${actions[action]??action.toLowerCase()} ${entities[entity]??entity.toLowerCase()}`;
  }
  async function loadAdminData(){
    try{const range=reportRange();const [users,vaccineList,lots,insurance,audit,management]=await Promise.all([adminApi.users(),adminApi.vaccines(),adminApi.lots(),adminApi.insurances(),adminApi.audit(),adminApi.report(range.start,range.end)]);report=management;
      usersData=users.map(u=>({id:String(u.id),name:u.nome,email:u.email,cpf:u.cpf,phone:u.telefone,birth:u.dataNascimento,role:{PACIENTE:'Paciente',FUNCIONARIO:'Funcionário',ADMINISTRADOR:'Administrador'}[u.perfil],registration:u.matricula??'',lastAccess:u.ultimoAcessoEm?new Date(u.ultimoAcessoEm).toLocaleString('pt-BR'):'Ainda não acessou',status:u.status==='ATIVO'?'Ativo':u.status}));
      vaccinesData=vaccineList.map(v=>({id:String(v.id),name:v.nome,manufacturer:v.fabricante,description:v.descricao,category:v.categoria,age:v.indicacao,ageMin:String(v.idadeMinimaMeses??0),ageMax:String(v.idadeMaximaMeses??''),doses:v.esquemaDoses,doseCount:String(v.numeroDoses??1),intervalDays:String(v.intervaloDias??''),boosterMonths:String(v.reforcoMeses??''),price:String(v.valorBase),status:v.ativo?'Ativo':'Inativo'}));
      batchesData=lots.map(l=>{const v=vaccineList.find(item=>item.id===l.vacinaId);return{id:String(l.id),number:l.numeroLote,vaccine:v?.nome??`Vacina #${l.vacinaId}`,manufacturer:v?.fabricante??'',expires:l.dataValidade,quantity:String(l.quantidadeAtual),initialQuantity:String(l.quantidadeInicial),supplier:l.fornecedor,status:l.status==='ATIVO'?'Regular':'Inativo'};});
      insuranceData=insurance.map(i=>({id:String(i.id),company:i.nome,plan:i.plano,code:i.codigoOperacional,coverageType:i.tipoCobertura,discount:String(i.percentualDesconto??''),copay:String(i.valorCoparticipacao??''),status:i.ativo?'Ativo':'Inativo'}));
      auditData=audit.map((e:any)=>{const responsible=users.find(user=>user.id===e.usuarioId);return{rawDate:e.criadoEm??'',date:e.criadoEm?new Date(e.criadoEm).toLocaleString('pt-BR'):'Data não informada',user:responsible?.nome??(e.usuarioId?`Usuário removido (#${e.usuarioId})`:'Sistema'),action:humanAction(e.acao??'',e.entidade??''),actionCode:e.acao??'',resource:`${e.entidade??'Registro'} #${e.entidadeId??'—'}`,details:e.descricao??'Nenhum detalhe adicional registrado.',ip:e.ip??'Não registrado'};});
    }catch(exception){toast=exception instanceof Error?exception.message:'Não foi possível carregar os dados administrativos.';}
  }
  onMount(loadAdminData);
  function exportReport() {
    const range=reportRange();
    const escape=(value:string|number)=>`"${String(value).replaceAll('"','""')}"`;
    const rows:(string|number)[][]=[
      ['RELATÓRIO GERENCIAL ORBE'],['Período',new Date(`${range.start}T12:00:00`).toLocaleDateString('pt-BR'),new Date(`${range.end}T12:00:00`).toLocaleDateString('pt-BR')],[],
      ['RESUMO OPERACIONAL'],['Indicador','Valor'],['Pacientes ativos',report.pacientesAtivos],['Agendamentos',report.agendamentosTotal],['Aplicações',report.aplicacoesPeriodo],['Concluídos',report.concluidos],['Faltas',report.faltas],['Cancelados',report.cancelados],['Taxa de comparecimento',`${attendanceRate().toFixed(1).replace('.',',')}%`],['Receita registrada',report.receita.toFixed(2).replace('.',',')],['Doses em estoque',report.dosesEstoque],['Doses perdidas',report.dosesPerdidas],['Lotes em alerta',report.lotesAlerta],[],
      ['VACINAS MAIS APLICADAS'],['Vacina','Aplicações'],...report.vacinasMaisAplicadas.map(item=>[item.vacina,item.quantidade]),[],
      ['PRODUÇÃO POR PROFISSIONAL'],['Profissional','Aplicações'],...report.profissionaisMaisAtivos.map(item=>[item.vacina,item.quantidade]),[],
      ['APLICAÇÕES POR SEMANA'],['Semana','Aplicações'],...report.aplicacoesPorSemana.map(item=>[item.periodo,item.quantidade]),[],
      ['ALERTAS DE ESTOQUE'],['Vacina','Lote','Tipo','Quantidade','Validade'],...report.alertas.map(item=>[item.vacina,item.lote,item.tipo,item.quantidade,item.validade]),
    ];
    const csv = ['sep=;',...rows.map(row=>row.map(escape).join(';'))].join('\r\n');
    const url = URL.createObjectURL(new Blob([`\uFEFF${csv}`], { type: 'text/csv;charset=utf-8' }));
    const link = document.createElement('a');
    link.href = url;
    link.download = `relatorio-orbe-${range.start}-a-${range.end}.csv`;
    link.click();
    URL.revokeObjectURL(url);
    toast = 'Relatório exportado em CSV.';
  }
  function attendanceRate() {
    const considered=report.concluidos+report.faltas;
    return considered ? report.concluidos/considered*100 : 0;
  }
  function weekLabel(value:string){const match=value.match(/(\d{4})-S(\d+)/);return match?`Sem. ${Number(match[2])}`:value;}
  function exportSelected(){if(reportFormat==='print'){window.print();return;}exportReport();}
  function exportAudit(){const esc=(v:string)=>`"${v.replaceAll('"','""')}"`;const rows=[['Quando','Responsável','Evento','Registro','Detalhes','Origem'],...filteredAudit.map(e=>[e.date,e.user,e.action,e.resource,e.details,e.ip])];const url=URL.createObjectURL(new Blob([`\uFEFFsep=;\r\n${rows.map(r=>r.map(esc).join(';')).join('\r\n')}`],{type:'text/csv;charset=utf-8'}));const a=document.createElement('a');a.href=url;a.download='auditoria-orbe.csv';a.click();URL.revokeObjectURL(url);}
</script>

<div class="page compact" class:scrollable={mode === 'reports' || mode === 'audit'}>
  {#if mode === 'dashboard'}<PageHeader
      eyebrow="Administração"
      title="Visão gerencial"
      description={`Resumo da clínica em ${new Date().toLocaleDateString('pt-BR')}.`}
    />
    <div class="stats">
      {#each [{ n: String(report.pacientesAtivos), l: 'Pacientes ativos', d: 'Cadastros habilitados' }, { n: String(report.aplicacoesPeriodo), l: 'Aplicações no período', d: 'Últimos 30 dias' }, { n: String(report.dosesEstoque), l: 'Doses em estoque', d: 'Lotes ativos e válidos' }, { n: String(report.lotesAlerta), l: 'Alertas de estoque', d: 'Requer atenção' }] as item}<Card
          ><div class="stat"><span>{item.l}</span><strong>{item.n}</strong><small>{item.d}</small></div></Card
        >{/each}
    </div>
    <div class="dashboard">
      <Card
        ><div class="card-head">
          <div class="inline-title">
            <h2>Aplicações por semana</h2>
            <Tooltip text="Últimas cinco semanas" />
          </div>
          <button onclick={() => onNavigate('admin-reports')}>Ver relatório</button>
        </div>
        <div class="chart">
          {#each report.aplicacoesPorSemana as bar}<div
            >
              <strong>{bar.quantidade}</strong><span title={`${bar.quantidade} aplicações em ${weekLabel(bar.periodo)}`} style={`height:${Math.max(5,bar.quantidade/Math.max(1,...report.aplicacoesPorSemana.map(item=>item.quantidade))*100)}%`}></span><small>{weekLabel(bar.periodo)}</small>
            </div>{/each}
          {#if report.aplicacoesPorSemana.length===0}<p class="chart-empty">Nenhuma aplicação registrada nas últimas semanas.</p>{/if}
        </div></Card
      ><Card
        ><div class="card-head">
          <div class="inline-title">
            <h2>Alertas prioritários</h2>
            <Tooltip text="Estoque e operação" />
          </div>
        </div>
        <div class="alerts">
          {#each report.alertas as alert}<button onclick={() => onNavigate('admin-stock')}><i class={alert.tipo==='VALIDADE'?'warning':'danger'}>!</i><span><strong>{alert.vacina}</strong><small>{alert.lote} · {alert.tipo==='VALIDADE'?`validade ${new Date(`${alert.validade}T12:00:00`).toLocaleDateString('pt-BR')}`:`${alert.quantidade} doses`}</small></span><b>›</b></button>{/each}
          {#if report.alertas.length===0}<button><i class="success">✓</i><span><strong>Sem alertas prioritários</strong><small>Estoque dentro dos parâmetros</small></span></button>{/if}
        </div></Card
      >
    </div>
    <div class="quick">
      {#each [{ i: '♧', t: 'Gerenciar usuários', p: 'admin-users' }, { i: '✚', t: 'Nova vacina', p: 'admin-vaccines' }, { i: '▤', t: 'Entrada de lote', p: 'admin-stock' }, { i: '◇', t: 'Gerar relatório', p: 'admin-reports' }] as item}<button
          onclick={() => onNavigate(item.p)}><span>{item.i}</span><strong>{item.t}</strong></button
        >{/each}
    </div>
  {:else}<PageHeader eyebrow={title![0]} title={title![1]} description={title![2]}
      >{#snippet actions()}{#if mode === 'users'}<Button onclick={() => openCreate('user')}>Novo acesso</Button
          >{:else if mode === 'vaccines'}<Button onclick={() => openCreate('vaccine')}>Nova vacina</Button
          >{:else if mode === 'stock'}<Button onclick={() => openCreate('batch')}>Entrada de lote</Button
          >{:else if mode === 'insurance'}<Button onclick={() => openCreate('insurance')}>Novo convênio</Button
          >{:else if mode === 'reports'}<Button onclick={exportSelected}>Gerar relatório</Button
          >{/if}{/snippet}</PageHeader
    >
    {#if mode === 'reports'}<div class="report-filters">
        <label>Período<select bind:value={reportDays} onchange={loadReport}><option value={7}>Últimos 7 dias</option><option value={30}>Últimos 30 dias</option><option value={90}>Últimos 90 dias</option><option value={365}>Últimos 12 meses</option></select></label>
        <label>Formato<select bind:value={reportFormat}><option value="csv">CSV para Excel</option><option value="print">Impressão / PDF</option></select></label>
        <p class="filter-note">Todos os dados abaixo respeitam o período selecionado.</p>
      </div>
      <div class="stats reports">
        {#each [{ n: String(report.aplicacoesPeriodo), l: 'Aplicações realizadas', d: `Últimos ${reportDays} dias` }, { n: `${attendanceRate().toFixed(1).replace('.',',')}%`, l: 'Comparecimento', d: `${report.concluidos} realizados e ${report.faltas} faltas` }, { n: report.receita.toLocaleString('pt-BR',{style:'currency',currency:'BRL'}), l: 'Receita registrada', d: 'Valores das aplicações' }, { n: String(report.lotesAlerta), l: 'Lotes que exigem atenção', d: `${report.dosesEstoque} doses disponíveis` }] as item}<Card
            ><div class="stat"><span>{item.l}</span><strong>{item.n}</strong><small>{item.d}</small></div></Card
          >{/each}
      </div>
      <div class="report-grid">
        <Card
          ><h2>Vacinas mais aplicadas</h2>
          {#each report.vacinasMaisAplicadas as row}<div
              class="metric"
            >
              <span><b>{row.vacina}</b><small>{row.quantidade} aplicações</small></span><i><em style={`width:${row.quantidade/Math.max(1,...report.vacinasMaisAplicadas.map(item=>item.quantidade))*100}%`}></em></i>
            </div>{/each}</Card
        ><Card
          ><h2>Situação dos agendamentos</h2>
          <div class="donut" style={`--completed:${report.agendamentosTotal?report.concluidos/report.agendamentosTotal*100:0}%;--missed:${report.agendamentosTotal?(report.concluidos+report.faltas)/report.agendamentosTotal*100:0}%`}><div>{report.agendamentosTotal}<small>agendamentos</small></div></div>
          <div class="legend">
            <span><i class="green"></i>Concluídos <b>{report.concluidos}</b></span><span><i class="yellow"></i>Faltas <b>{report.faltas}</b></span><span><i class="gray"></i>Cancelados <b>{report.cancelados}</b></span>
          </div></Card
        >
      </div>
      <div class="report-details">
        <Card><h2>Resumo operacional</h2><div class="operation-table">
          <span>Agendamentos no período<strong>{report.agendamentosTotal}</strong></span>
          <span>Concluídos<strong>{report.concluidos}</strong></span>
          <span>Não compareceram<strong>{report.faltas}</strong></span>
          <span>Cancelados<strong>{report.cancelados}</strong></span>
          <span>Doses perdidas ou vencidas<strong>{report.dosesPerdidas}</strong></span>
          <span>Pacientes ativos na clínica<strong>{report.pacientesAtivos}</strong></span>
        </div></Card>
        <Card><h2>Profissionais com mais aplicações</h2><div class="report-alert-list">
          {#each report.profissionaisMaisAtivos as item}<div><span><strong>{item.vacina}</strong><small>Aplicações registradas no período</small></span><b>{item.quantidade}</b></div>{/each}
          {#if report.profissionaisMaisAtivos.length===0}<p>Nenhuma aplicação no período selecionado.</p>{/if}
        </div></Card><Card><h2>Alertas de estoque</h2><div class="report-alert-list">
          {#each report.alertas as alert}<div><span><strong>{alert.vacina}</strong><small>Lote {alert.lote} · validade {new Date(`${alert.validade}T12:00:00`).toLocaleDateString('pt-BR')}</small></span><b>{alert.tipo==='VALIDADE'?'Validade próxima':`${alert.quantidade} doses`}</b></div>{/each}
          {#if report.alertas.length===0}<p>Nenhum lote exige atenção neste momento.</p>{/if}
        </div></Card>
      </div>
    {:else if mode === 'stock'}<div class="tabs">
        <button class:active={stockTab === 'batches'} onclick={() => (stockTab = 'batches')}>Lotes</button><button
          class:active={stockTab === 'movements'}
          onclick={() => (stockTab = 'movements')}>Movimentações</button
        >
      </div>
      {#if stockTab === 'batches'}{@render DataTable(
          ['Lote', 'Vacina', 'Validade', 'Quantidade', 'Situação', ''],
          batchesData.map((b) => [
            b.number,
            `${b.vaccine}|${b.manufacturer}`,
            b.expires,
            b.quantity,
            b.status,
            'Editar',
          ]),
          true,
        )}{:else}{@render DataTable(
          ['Data', 'Lote', 'Movimentação', 'Quantidade', 'Responsável'],
          movements.map((m) => [m.date, m.batch, m.type, m.quantity, m.user]),
        )}{/if}
    {:else if mode === 'users'}{@render Toolbar()}{@render DataTable(
        ['Nome', 'E-mail', 'Perfil', 'Último acesso', 'Situação', ''],
        usersData
          .filter((u) => [u.name,u.email,u.cpf].some(value=>value.toLowerCase().includes(query.toLowerCase())) && (statusFilter==='Todos'||u.status===statusFilter) && (roleFilter==='Todos'||u.role===roleFilter))
          .map((u) => [u.name, u.email, u.role, u.lastAccess ?? 'Ainda não acessou', u.status, 'Editar']),
        true,
      )}
    {:else if mode === 'vaccines'}{@render Toolbar()}{@render DataTable(
        ['Vacina', 'Fabricante', 'Indicação', 'Valor-base', 'Situação', ''],
        vaccinesData
          .filter((v) => [v.name,v.manufacturer,v.category].some(value=>value.toLowerCase().includes(query.toLowerCase())) && (statusFilter==='Todos'||v.status===statusFilter))
          .slice((page - 1) * pageSize, page * pageSize)
          .map((v) => [
            v.name,
            v.manufacturer,
            v.age,
            Number(v.price).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' }),
            v.status,
            'Editar',
          ]),
        true,
      )}{@render Pagination(Math.ceil(vaccinesData.length / pageSize))}
    {:else if mode === 'insurance'}{@render Toolbar()}{@render DataTable(
        ['Convênio', 'Plano', 'Código', 'Cobertura', 'Situação', ''],
        insuranceData.map((i) => [i.company, i.plan, i.code, i.coverageType, i.status, 'Editar']),
        true,
      )}
    {:else if mode === 'audit'}<div class="audit-filter">
        <div class="toolbar"><input placeholder="Buscar por pessoa, ação ou detalhe" value={query} oninput={(e)=>(query=e.currentTarget.value)} /></div><div class="audit-actions"><button onclick={exportAudit}>Exportar CSV</button><button onclick={()=>window.print()}>Imprimir / PDF</button><button onclick={() => (advancedFilters = !advancedFilters)}
          >{advancedFilters ? 'Ocultar filtros' : 'Filtros avançados'}</button
        ></div>
      </div>
      {#if advancedFilters}<div class="report-filters">
          <label>Ação<select bind:value={auditAction}><option value="TODAS">Todas as ações</option><option value="CRIAR">Registros criados</option><option value="ATUALIZAR">Registros atualizados</option><option value="CANCELAR">Cancelamentos</option><option value="REAGENDAR">Reagendamentos</option></select></label>
          <label>Período<select bind:value={auditPeriod}><option value={7}>Últimos 7 dias</option><option value={30}>Últimos 30 dias</option><option value={90}>Últimos 90 dias</option><option value={365}>Últimos 12 meses</option></select></label>
        </div>{/if}
      {@render DataTable(
        ['Quando', 'Responsável', 'O que aconteceu', 'Detalhes', 'Origem'],
        filteredAudit.map((e) => [e.date, e.user, `${e.action}|${e.resource}`, e.details, e.ip]),
      )}{/if}
  {/if}
</div>
{#if modalEntity}<AdminCrudDialog
    entity={modalEntity}
    initial={editIndex >= 0 ? currentRows(modalEntity)[editIndex] : {}}
    onSave={saveEntity}
    onCancel={() => {
      modalEntity = null;
      editIndex = -1;
    }}
    vaccineOptions={vaccinesData.map(item=>item.name)}
  />{/if}
{#if toast}<Toast message={toast} onClose={() => (toast = '')} />{/if}

{#snippet Toolbar()}<div class="toolbar">
    <input placeholder="Buscar registros" value={query} oninput={(e) => (query = e.currentTarget.value)} /><select
      bind:value={statusFilter}><option value="Todos">Todos os status</option><option value="Ativo">Ativos</option><option value="Inativo">Inativos</option><option value="Bloqueado">Bloqueados</option></select
    >{#if mode==='users'}<select bind:value={roleFilter}><option value="Todos">Todos os perfis</option><option>Paciente</option><option>Funcionário</option><option>Administrador</option></select>{/if}
  </div>{/snippet}
{#snippet DataTable(headers: string[], rows: string[][], editable = false)}
  <div class="admin-collection">
    <CollectionPanel title="Registros" description={`${rows.length} itens exibidos`}>
      {#snippet actions()}<ViewModeToggle bind:value={adminView} />{/snippet}
      {#if adminView === 'list'}<div class="table-wrap">
          <div class="table" style={`--columns:${headers.length}`}>
            {#each headers as header}<strong class="th">{header}</strong
              >{/each}{#each rows as row, rowIndex}{#each row as cell, index}{#if editable && index === row.length - 1 && cell === 'Editar'}<span
                    class="action"><button onclick={() => openEdit(rowIndex)}>Editar</button></span
                  >{:else}<span class:status={['Ativo', 'Inativo', 'Regular', 'Atenção', 'Crítico'].includes(cell)}
                    >{#each cell.split('|') as part, partIndex}{#if partIndex === 0}<b>{part}</b>{:else}<small
                          >{part}</small
                        >{/if}{/each}</span
                  >{/if}{/each}{/each}
          </div>
        </div>
      {:else}<div class="admin-card-grid">
          {#each rows as row, rowIndex}<article>
              {#each row as cell, index}
                {#if !(editable && index === row.length - 1 && cell === 'Editar') && headers[index]}
                  <div>
                    <small>{headers[index]}</small><strong>{cell.split('|')[0]}</strong>{#if cell.includes('|')}<span
                        >{cell.split('|')[1]}</span
                      >{/if}
                  </div>
                {/if}
              {/each}
              {#if editable}<button onclick={() => openEdit(rowIndex)}>Editar registro</button>{/if}
            </article>{/each}
        </div>{/if}
    </CollectionPanel>
  </div>
{/snippet}
{#snippet Pagination(total: number)}<div class="pagination">
    <span>Página {page} de {total}</span><button disabled={page === 1} onclick={() => (page -= 1)}>←</button><button
      disabled={page === total}
      onclick={() => (page += 1)}>→</button
    >
  </div>{/snippet}

<style>
  .page {
    width: min(100%, var(--content-max));
    height: 100vh;
    margin: 0 auto;
    overflow: hidden;
    padding: var(--space-6) var(--space-8);
  }
  .page.scrollable {
    overflow-y: auto;
  }
  .stats {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: var(--space-3);
    margin-top: var(--space-6);
  }
  .stat {
    display: grid;
    gap: 0.3rem;
  }
  .stat span {
    color: var(--text-secondary);
    font-size: var(--text-xs);
  }
  .stat strong {
    font-size: var(--text-2xl);
  }
  .stat small {
    color: var(--text-tertiary);
  }
  .dashboard {
    display: grid;
    grid-template-columns: 1.35fr 1fr;
    gap: var(--space-3);
    height: 19rem;
    margin-top: var(--space-4);
  }
  .dashboard :global(.card) {
    overflow: hidden;
  }
  .card-head {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
  }
  .inline-title {
    display: flex;
    align-items: center;
    gap: var(--space-2);
  }
  .card-head h2,
  .report-grid h2 {
    font-size: var(--text-md);
  }
  .card-head button {
    border: 0;
    background: transparent;
    color: var(--color-brand-500);
    font-size: var(--text-xs);
    font-weight: 750;
    cursor: pointer;
  }
  .chart {
    display: flex;
    height: 12.5rem;
    align-items: flex-end;
    justify-content: space-around;
    gap: var(--space-3);
    padding-top: var(--space-4);
  }
  .chart > div {
    display: flex;
    height: 100%;
    flex: 1;
    flex-direction: column;
    justify-content: flex-end;
    gap: 0.4rem;
    text-align: center;
  }
  .chart span {
    display: block;
    width: 70%;
    max-width: 3rem;
    margin: auto auto 0;
    border-radius: 0.35rem 0.35rem 0 0;
    background: var(--color-brand-500);
  }
  .chart small {
    color: var(--text-tertiary);
  }
  .chart strong{font-size:var(--text-xs)}.chart-empty{align-self:center;margin:auto;color:var(--text-secondary);font-size:var(--text-sm)}
  .alerts {
    margin-top: var(--space-3);
  }
  .alerts button {
    display: grid;
    width: 100%;
    grid-template-columns: auto 1fr auto;
    align-items: center;
    gap: var(--space-3);
    border: 0;
    border-top: 1px solid var(--border-subtle);
    background: transparent;
    padding: var(--space-3) 0;
    color: var(--text-primary);
    text-align: left;
    cursor: pointer;
  }
  .alerts i {
    display: grid;
    width: 1.8rem;
    height: 1.8rem;
    place-items: center;
    border-radius: 50%;
    font-style: normal;
    font-weight: 800;
  }
  .alerts i.danger {
    background: var(--status-danger-bg);
    color: var(--status-danger);
  }
  .alerts i.warning {
    background: var(--status-warning-bg);
    color: var(--status-warning);
  }
  .alerts i.success {
    background: var(--status-success-bg);
    color: var(--status-success);
  }
  .alerts span {
    display: grid;
    gap: 0.15rem;
  }
  .alerts strong {
    font-size: var(--text-xs);
  }
  .alerts small {
    color: var(--text-tertiary);
  }
  .quick {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: var(--space-3);
    margin-top: var(--space-4);
  }
  .quick button {
    display: flex;
    align-items: center;
    gap: var(--space-3);
    border: 1px solid var(--border-subtle);
    border-radius: var(--radius-md);
    background: var(--surface-card);
    padding: var(--space-4);
    color: var(--text-primary);
    cursor: pointer;
  }
  .quick span {
    color: var(--color-brand-500);
    font-size: 1.2rem;
  }
  .quick strong {
    font-size: var(--text-xs);
  }
  .toolbar,
  .report-filters {
    display: flex;
    gap: var(--space-3);
    margin: var(--space-6) 0 var(--space-3);
  }
  input,
  select {
    min-height: 2.55rem;
    border: 1px solid var(--border-strong);
    border-radius: var(--radius-sm);
    background: var(--surface-card);
    padding: 0 var(--space-3);
    color: var(--text-primary);
    font-size: var(--text-sm);
  }
  .toolbar input {
    width: min(100%, 23rem);
  }
  .table-wrap {
    max-height: calc(100vh - 13rem);
    overflow: auto;
    border: 1px solid var(--border-subtle);
    border-radius: var(--radius-md);
    background: var(--surface-card);
  }
  .admin-collection {
    margin-top: var(--space-3);
  }
  .admin-card-grid {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: var(--space-4);
  }
  .admin-card-grid article {
    display: grid;
    gap: var(--space-3);
    border: 1px solid var(--border-subtle);
    border-radius: var(--radius-md);
    padding: var(--space-4);
  }
  .admin-card-grid article > div {
    display: grid;
    gap: 0.2rem;
  }
  .admin-card-grid small {
    color: var(--text-tertiary);
    font-size: var(--text-xs);
  }
  .admin-card-grid strong {
    font-size: var(--text-sm);
  }
  .admin-card-grid span {
    color: var(--text-secondary);
    font-size: var(--text-xs);
  }
  .admin-card-grid button {
    justify-self: start;
    border: 0;
    background: transparent;
    padding: 0;
    color: var(--color-brand-500);
    font-weight: 700;
    cursor: pointer;
  }
  .table {
    display: grid;
    min-width: 55rem;
    grid-template-columns: repeat(var(--columns), minmax(7rem, 1fr));
    align-items: center;
  }
  .table > * {
    min-height: 3.65rem;
    border-bottom: 1px solid var(--border-subtle);
    padding: var(--space-3);
    font-size: var(--text-xs);
  }
  .th {
    position: sticky;
    z-index: 1;
    top: 0;
    min-height: 2.6rem;
    background: var(--surface-subtle);
    color: var(--text-secondary);
    font-size: 0.68rem;
  }
  .table span {
    display: flex;
    flex-direction: column;
    justify-content: center;
    gap: 0.15rem;
  }
  .table span > b {
    font-weight: 650;
  }
  .table span small {
    color: var(--text-secondary);
  }
  .table .status b {
    align-self: flex-start;
    border-radius: var(--radius-pill);
    background: var(--surface-subtle);
    padding: 0.25rem 0.5rem;
  }
  .table .action {
    color: var(--color-brand-500);
    font-weight: 750;
  }
  .tabs {
    display: flex;
    gap: var(--space-5);
    margin: var(--space-6) 0 var(--space-3);
    border-bottom: 1px solid var(--border-subtle);
  }
  .tabs button {
    border: 0;
    border-bottom: 2px solid transparent;
    background: transparent;
    padding: 0 0 var(--space-3);
    color: var(--text-secondary);
    font-weight: 700;
    cursor: pointer;
  }
  .tabs button.active {
    border-color: var(--color-brand-500);
    color: var(--color-brand-500);
  }
  .pagination {
    display: flex;
    align-items: center;
    justify-content: flex-end;
    gap: var(--space-2);
    margin-top: var(--space-3);
    font-size: var(--text-xs);
  }
  .pagination span {
    margin-right: var(--space-2);
    color: var(--text-secondary);
  }
  .pagination button {
    width: 2rem;
    height: 2rem;
    border: 1px solid var(--border-subtle);
    border-radius: var(--radius-sm);
    background: var(--surface-card);
    color: var(--text-primary);
  }
  .report-filters label {
    display: grid;
    gap: 0.25rem;
    color: var(--text-secondary);
    font-size: var(--text-xs);
  }
  .stats.reports {
    margin-top: var(--space-4);
  }
  .report-grid {
    display: grid;
    grid-template-columns: 1.2fr 1fr;
    gap: var(--space-3);
    min-height: 18rem;
    margin-top: var(--space-4);
  }
  .metric {
    display: grid;
    grid-template-columns: 9rem 1fr;
    align-items: center;
    gap: var(--space-3);
    margin-top: var(--space-5);
  }
  .metric span {
    display: grid;
    gap: 0.15rem;
  }
  .metric b {
    font-size: var(--text-xs);
  }
  .metric small {
    color: var(--text-tertiary);
  }
  .metric > i {
    height: 0.5rem;
    overflow: hidden;
    border-radius: var(--radius-pill);
    background: var(--surface-subtle);
  }
  .metric em {
    display: block;
    height: 100%;
    border-radius: inherit;
    background: var(--color-brand-500);
  }
  .donut {
    display: grid;
    width: 9rem;
    height: 9rem;
    margin: var(--space-5) auto;
    place-items: center;
    border-radius: 50%;
    background: conic-gradient(
      var(--color-brand-500) 0 var(--completed),
      var(--status-warning) var(--completed) var(--missed),
      var(--border-strong) var(--missed) 100%
    );
  }
  .donut > div {
    display: grid;
    width: 6rem;
    height: 6rem;
    place-items: center;
    border-radius: 50%;
    background: var(--surface-card);
    font-size: var(--text-xl);
    font-weight: 800;
  }
  .donut small {
    display: block;
    color: var(--text-tertiary);
    font-size: var(--text-xs);
  }
  .legend {
    display: flex;
    justify-content: center;
    gap: var(--space-4);
    font-size: var(--text-xs);
  }
  .legend span {
    display: flex;
    align-items: center;
    gap: 0.3rem;
  }
  .legend i {
    width: 0.5rem;
    height: 0.5rem;
    border-radius: 50%;
    background: var(--border-strong);
  }
  .legend i.green {
    background: var(--color-brand-500);
  }
  .legend i.yellow {
    background: var(--status-warning);
  }
  .audit-filter {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }
  .audit-filter button {
    border: 0;
    background: transparent;
    color: var(--color-brand-500);
    font-weight: 700;
  }
  .audit-actions{display:flex;gap:var(--space-4)}
  .filter-note {
    align-self: end;
    margin-bottom: 0.65rem;
    color: var(--text-tertiary);
    font-size: var(--text-xs);
  }
  .report-details {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: var(--space-3);
    margin-top: var(--space-4);
    padding-bottom: var(--space-6);
  }
  .report-details h2 {
    margin-bottom: var(--space-3);
    font-size: var(--text-md);
  }
  .operation-table {
    display: grid;
  }
  .operation-table span,
  .report-alert-list > div {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: var(--space-4);
    border-top: 1px solid var(--border-subtle);
    padding: var(--space-3) 0;
    color: var(--text-secondary);
    font-size: var(--text-xs);
  }
  .operation-table strong,
  .report-alert-list b {
    color: var(--text-primary);
    white-space: nowrap;
  }
  .report-alert-list span {
    display: grid;
    gap: 0.2rem;
  }
  .report-alert-list small,
  .report-alert-list p {
    color: var(--text-tertiary);
    font-size: var(--text-xs);
  }
  .report-alert-list b {
    border-radius: var(--radius-pill);
    background: var(--status-warning-bg);
    padding: 0.3rem 0.55rem;
    color: var(--status-warning);
    font-size: var(--text-xs);
  }
  @media (max-width: 1050px) {
    .admin-card-grid {
      grid-template-columns: repeat(2, minmax(0, 1fr));
    }
    .page {
      height: auto;
      min-height: 100vh;
      overflow: visible;
    }
    .stats {
      grid-template-columns: repeat(2, 1fr);
    }
    .dashboard,
    .report-grid,
    .report-details {
      height: auto;
      grid-template-columns: 1fr;
    }
    .quick {
      grid-template-columns: repeat(2, 1fr);
    }
    .table-wrap {
      max-height: 60vh;
    }
  }
  @media (max-width: 680px) {
    .admin-card-grid {
      grid-template-columns: 1fr;
    }
    .page {
      padding: var(--space-5);
    }
    .stats,
    .quick {
      grid-template-columns: 1fr;
    }
    .toolbar,
    .report-filters {
      align-items: stretch;
      flex-direction: column;
    }
    .toolbar input {
      width: 100%;
    }
  }
</style>
