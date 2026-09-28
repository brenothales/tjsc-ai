import { Routes } from '@angular/router';

export const chatRoutes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./components/chat-page/chat-page').then((m) => m.ChatPageComponent),
  },
  {
    path: ':id',
    loadComponent: () =>
      import('./components/chat-page/chat-page').then((m) => m.ChatPageComponent),
  },
];
