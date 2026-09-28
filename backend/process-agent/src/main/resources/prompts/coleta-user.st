Colete TODOS os dados do processo {numero} chamando as ferramentas nesta ordem:
1. buscar_contexto_completo — dados gerais, partes e magistrado
2. buscar_peticao_inicial — fatos e pedidos do autor
3. buscar_documentos_com_texto com tipo="Contestação" — defesa do réu
4. buscar_movimentacoes — histórico processual
5. buscar_documentos_com_texto sem filtro — outros documentos relevantes

Após chamar todas, organize em:
## IDENTIFICAÇÃO
(número, classe, comarca, vara, valor da causa, situação)
## PARTES
(nome e polo — sem CPF/CNPJ)
## MAGISTRADO
## FATOS (Petição Inicial)
(síntese fiel dos fatos e pedidos)
## DEFESA
(contestação — se houver)
## HISTÓRICO PROCESSUAL
(movimentações relevantes em ordem cronológica)
## OUTROS DOCUMENTOS
(lista dos demais documentos)

Seja completo e factual. Esta é a fase de coleta — não faça análise jurídica.
