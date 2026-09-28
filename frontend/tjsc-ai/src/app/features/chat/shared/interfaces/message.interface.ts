import { MessageRole } from '../enums/message-role.enum';

export interface Message {
  id: string;
  role: MessageRole;
  content: string;
  timestamp: Date;
  streaming?: boolean;
  insightKey?: string;
  fromCache?: boolean;
}

export interface ChatRequest {
  conversationId: string;
  message: string;
  insightKey?: string;
  bypassCache?: boolean;
  model?: string;
  temperature?: number;
  systemExtra?: string;
}
