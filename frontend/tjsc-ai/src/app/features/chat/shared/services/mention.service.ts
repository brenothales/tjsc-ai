import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { catchError, forkJoin, map, Observable, of } from 'rxjs';

export type MentionType = 'processo' | 'magistrado' | 'comarca';

export interface MentionResult {
  type: MentionType;
  label: string;
  sublabel?: string;
  value: string;
}

const SQL_URL = 'http://localhost:8081/api/v1/sql';
const PESQUISAR_URL = 'http://localhost:8081/api/v1/processos/pesquisar';

@Injectable({ providedIn: 'root' })
export class MentionService {
  private readonly http = inject(HttpClient);

  search(query: string): Observable<MentionResult[]> {
    const q = query.trim();

    return forkJoin([
      this.searchProcessos(q),
      this.searchMagistrados(q),
      this.searchComarcas(q),
    ]).pipe(
      map(([p, m, c]) => {
        const seen = new Set<string>();
        return [...p, ...m, ...c].filter((r) => {
          const key = r.value
            .toLowerCase()
            .normalize('NFD')
            .replace(/[\u0300-\u036f]/g, '');
          if (seen.has(key)) return false;
          seen.add(key);
          return true;
        });
      })
    );
  }

  private searchProcessos(q: string): Observable<MentionResult[]> {
    return this.http
      .get<any>(`${PESQUISAR_URL}?parte=${encodeURIComponent(q)}&size=4`)
      .pipe(
        map((res) =>
          (res.data ?? []).map((p: any) => ({
            type: 'processo' as const,
            label: p.numero,
            sublabel: `${p.classe} · ${p.comarca}`,
            value: `@processo:${p.numero}`,
          }))
        ),
        catchError(() => of([]))
      );
  }

  private searchMagistrados(q: string): Observable<MentionResult[]> {
    const safe = q.replace(/'/g, "''");
    const sql = `SELECT MIN(nome) as nome FROM magistrado WHERE LOWER(nome) LIKE LOWER('%${safe}%') GROUP BY LOWER(nome) LIMIT 4`;
    return this.http.post<any>(SQL_URL, { sql }).pipe(
      map((res) =>
        (res.rows ?? []).map((r: any) => ({
          type: 'magistrado' as const,
          label: r.nome,
          sublabel: 'Magistrado',
          value: `@magistrado:${r.nome}`,
        }))
      ),
      catchError(() => of([]))
    );
  }

  private searchComarcas(q: string): Observable<MentionResult[]> {
    const safe = q.replace(/'/g, "''");
    const sql = `SELECT MIN(comarca) as comarca FROM processo WHERE LOWER(comarca) LIKE LOWER('%${safe}%') GROUP BY LOWER(comarca) ORDER BY comarca LIMIT 4`;
    return this.http.post<any>(SQL_URL, { sql }).pipe(
      map((res) =>
        (res.rows ?? []).map((r: any) => ({
          type: 'comarca' as const,
          label: r.comarca,
          sublabel: 'Comarca',
          value: `@comarca:${r.comarca}`,
        }))
      ),
      catchError(() => of([]))
    );
  }
}
