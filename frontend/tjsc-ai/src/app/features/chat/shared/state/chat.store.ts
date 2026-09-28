// Re-exported signal state lives in ChatService to avoid duplication.
// This file is reserved for future NgRx SignalStore migration if state grows.
export { ChatService as ChatStore } from '../services/chat.service';
