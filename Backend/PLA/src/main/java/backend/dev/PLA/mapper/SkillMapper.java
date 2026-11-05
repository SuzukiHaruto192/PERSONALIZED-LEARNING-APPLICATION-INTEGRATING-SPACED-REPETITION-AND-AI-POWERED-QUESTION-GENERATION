package backend.dev.PLA.mapper;

import backend.dev.PLA.dto.request.Quiz.SkillRequest;
import backend.dev.PLA.dto.response.quiz.SkillResponse;
import backend.dev.PLA.entities.quiz.Skill;

public class SkillMapper {
    public static Skill toSkill(SkillRequest skillRequest) {
        return Skill.builder()
                .name(skillRequest.getName())
                .code(skillRequest.getCode())
                .icon(skillRequest.getIcon())
                .build();
    }

    public static SkillResponse toSkillResponse(Skill skill) {
        return SkillResponse.builder()
                .name(skill.getName())
                .id(skill.getId())
                .code(skill.getCode())
                .icon(skill.getIcon())
                .build();
    }

    public static void updateSkill(Skill skill, SkillRequest skillRequest) {
        skill.setName(skillRequest.getName());
        skill.setCode(skillRequest.getCode());
        skill.setIcon(skillRequest.getIcon());
    }
}
