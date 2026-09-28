import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API } from '../constants/api.constants';
import { ConversationDetail, ConversationPage } from '../interfaces/conversation.interface';

@Injectable({ providedIn: 'root' })
export class ConversationApiService {
  private readonly http = inject(HttpClient);

  list(page = 0, size = 20): Observable<ConversationPage> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<ConversationPage>(API.conversations, { params });
  }

  get(id: string): Observable<ConversationDetail> {
    return this.http.get<ConversationDetail>(API.conversationById(id));
  }

  updateTitle(id: string, title: string): Observable<void> {
    return this.http.patch<void>(API.conversationTitle(id), { title });
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(API.conversationById(id));
  }

  setStarred(id: string, starred: boolean): Observable<void> {
    return this.http.patch<void>(API.conversationById(id) + '/starred', { starred });
  }

  setArchived(id: string, archived: boolean): Observable<void> {
    return this.http.patch<void>(API.conversationById(id) + '/archived', { archived });
  }
}
