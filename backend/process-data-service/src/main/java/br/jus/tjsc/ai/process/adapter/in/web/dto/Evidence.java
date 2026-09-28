package br.jus.tjsc.ai.process.adapter.in.web.dto;

public record Evidence(
        String type,
        String id,
        String description,
        String processNumber
) {
    public static Evidence of(String type, Long id, String description, String processNumber) {
        return new Evidence(type, String.valueOf(id), description, processNumber);
    }
}
