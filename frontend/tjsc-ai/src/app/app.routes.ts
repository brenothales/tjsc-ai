import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: 'chat',
    loadChildren: () =>
      import('./features/chat/chat.routes').then((m) => m.chatRoutes),
  },
  {
    path: '',
    redirectTo: 'chat',
    pathMatch: 'full',
  },
];
