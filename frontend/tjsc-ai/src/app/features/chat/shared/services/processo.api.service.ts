import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ProcessoDto {
  numero: string;
  classe: string;
  assunto: string;
  comarca: string;
  dataAutuacao: string;
  dataSentenca: string | null;
  valorCausa: number | null;
  situacao: string;
  situacaoNormalizada: string;
}

export interface ParteDto {
  id: number;
  nome: string;
  tipo: string;
  documento: string;
  dataCadastro: string;
  polo: string;
}

export interface MagistradoDto {
  id: number;
  nome: string;
  dataInicio: string;
  situacao: string;
}

export interface MovimentacaoDto {
  id: number;
  codigo: number;
  descricao: string;
  dataMov: string;
}

export interface DocumentoTextoDto {
  id: number;
  tipo: string;
  autor: string;
  dataJuntada: string;
  numPaginas: number;
  texto: string;
}

export interface ContextoResponse {
  processo: ProcessoDto;
  situacaoNormalizada: string;
  partes: ParteDto[];
  magistrados: MagistradoDto[];
  movimentacoes: MovimentacaoDto[];
  sentenca: DocumentoTextoDto | null;
  peticaoInicial: DocumentoTextoDto | null;
}

export interface PagedResponse<T> {
  found: boolean;
  data: T[];
  message: string | null;
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

@Injectable({ providedIn: 'root' })
export class ProcessoApiService {
  private readonly http = inject(HttpClient);
  private readonly base = 'http://localhost:8081/api/v1/processos';

  buscarContexto(numero: string): Observable<ContextoResponse> {
    return this.http.get<ContextoResponse>(`${this.base}/${numero}/contexto`);
  }

  movimentacoes(numero: string, page = 0, size = 20): Observable<PagedResponse<MovimentacaoDto>> {
    return this.http.get<PagedResponse<MovimentacaoDto>>(
      `${this.base}/${numero}/movimentacoes?page=${page}&size=${size}`
    );
  }

  documentos(numero: string): Observable<PagedResponse<DocumentoTextoDto>> {
    return this.http.get<PagedResponse<DocumentoTextoDto>>(
      `${this.base}/${numero}/documentos/texto?page=0&size=50`
    );
  }
}
