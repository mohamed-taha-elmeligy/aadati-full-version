package com.mts.aadati.seed.seeders;

import com.mts.aadati.entities.HabitCategory;
import com.mts.aadati.repository.HabitCategoryRepository;
import com.mts.aadati.seed.Seeder;
import com.mts.aadati.services.HabitCategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class HabitCategorySeeder implements Seeder {

    private final HabitCategoryRepository habitCategoryRepository;
    private final HabitCategoryService habitCategoryService;

    private static final List<HabitCategory> DEFAULT_CATEGORIES = List.of(
            new HabitCategory("Fitness", "Activities to stay fit", "#FF5733"),
            new HabitCategory("Health", "Healthy habits & routines", "#33FF57"),
            new HabitCategory("Learning", "Study, courses, and skill improvement", "#3357FF"),
            new HabitCategory("Productivity", "Work, planning, and time management", "#FFC300"),
            new HabitCategory("Mindfulness", "Meditation, journaling, mental wellness", "#DAF7A6"),
            new HabitCategory("Finance", "Saving, budgeting, financial goals", "#900C3F"),
            new HabitCategory("Social", "Networking, family, friends", "#581845"),
            new HabitCategory("Nutrition", "Healthy eating and diet", "#FF6F61"),
            new HabitCategory("Hydration", "Drinking enough water", "#1E90FF"),
            new HabitCategory("Sleep", "Sleep routines and hygiene", "#8A2BE2"),
            new HabitCategory("Hobbies", "Creative and leisure activities", "#FF69B4"),
            new HabitCategory("Self-Care", "Personal care and relaxation", "#00CED1"),
            new HabitCategory("Chores", "Household tasks and cleaning", "#FFD700"),
            new HabitCategory("Environment", "Eco-friendly habits", "#32CD32"),
            new HabitCategory("Reading", "Books, articles, knowledge", "#FF8C00"),
            new HabitCategory("Tech Detox", "Limit screen time", "#8B0000"),
            new HabitCategory("Mindset", "Positive thinking & reflection", "#20B2AA"),
            new HabitCategory("Spirituality", "Spiritual growth and practices", "#BA55D3"),
            new HabitCategory("Creativity", "Art, music, writing", "#FF4500"),
            new HabitCategory("Volunteering", "Community service and helping others", "#2E8B57"),
            new HabitCategory("Career", "Professional development and career growth", "#34495E"),
            new HabitCategory("Communication", "Improve speaking, listening, and writing", "#16A085"),
            new HabitCategory("Language Learning", "Practice and improve foreign languages", "#2980B9"),
            new HabitCategory("Fitness Recovery", "Stretching, rest, and muscle recovery", "#E67E22"),
            new HabitCategory("Outdoor Activities", "Walking, hiking, and spending time outdoors", "#27AE60"),
            new HabitCategory("Digital Skills", "Learn software, tools, and technical skills", "#6C5CE7"),
            new HabitCategory("Organization", "Organize belongings, files, and spaces", "#7F8C8D"),
            new HabitCategory("Time Management", "Build routines and manage time effectively", "#F39C12"),
            new HabitCategory("Relationships", "Strengthen personal relationships", "#E84393"),
            new HabitCategory("Family", "Spend quality time with family", "#D35400"),
            new HabitCategory("Personal Development", "Build useful skills and improve yourself", "#5D6D7E"),
            new HabitCategory("Exercise", "Follow workout plans and physical training", "#C0392B"),
            new HabitCategory("Mental Health", "Support emotional balance and mental well-being", "#9B59B6"),
            new HabitCategory("Cooking", "Practice cooking and prepare meals", "#E17055"),
            new HabitCategory("Gardening", "Care for plants and maintain a garden", "#6AB04C"),
            new HabitCategory("Pet Care", "Feed, groom, and care for pets", "#A0522D"),
            new HabitCategory("Digital Organization", "Manage emails, files, and digital clutter", "#0984E3"),
            new HabitCategory("Goal Setting", "Set goals, review progress, and plan ahead", "#B8860B"),
            new HabitCategory("Community", "Participate in community activities", "#008080"),
            new HabitCategory("Hygiene", "Maintain daily personal hygiene routines", "#48C9B0")
    );

    @Override
    public void seed() {
        if (habitCategoryRepository.count() > 0) {
            log.debug("Habit categories already seeded ({} existing), skipping",
                    habitCategoryRepository.count());
            return;
        }

        log.debug("Seeding default habit categories");
        for (HabitCategory category: DEFAULT_CATEGORIES)
            habitCategoryService.addCategory(category);

        log.info("Default habit categories seeded ({} categories)", DEFAULT_CATEGORIES.size());
    }

    @Override
    public int order() {
        return 30;
    }
}
