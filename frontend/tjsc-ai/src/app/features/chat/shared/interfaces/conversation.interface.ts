export interface ConversationSummary {
  id: string;
  title: string;
  createdAt: string;
  lastMessageAt: string;
  messageCount: number;
  starred: boolean;
  archived: boolean;
}

export interface ConversationPage {
  content: ConversationSummary[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}

export interface ConversationDetail {
  id: string;
  messages: ConversationMessage[];
}

export interface ConversationMessage {
  role: string;
  content: string;
  timestamp: string;
}
