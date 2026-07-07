package com.FreeRave.shredzilla.navigation

object AppRoutes {
    const val SPLASH = "splash"
    const val AUTH = "auth_flow"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val FORGOT_PASSWORD = "forgot_password"
    const val ONBOARDING = "onboarding_flow"
    const val GENDER_SELECTION = "gender_selection"
    const val PHYSICAL_DETAILS = "physical_details"
    const val REST_TIME_SELECTION = "rest_time_selection"
    const val INITIAL_EXERCISES_SELECTION = "initial_exercises_selection"
    const val WEEKLY_GOAL_SELECTION = "weekly_goal_selection"
    const val SETS = "sets" // Main screen for SetGraphScreen, also acts as a tab root
    const val EXERCISE_LIST_SCREEN = "exercise_list_screen"
    const val ADD_EXERCISE = "add_exercise"
    const val SETTINGS = "settings_screen"
    const val ACCOUNT_SETTINGS = "account_settings_screen"
    const val UPDATE_USERNAME = "update_username_screen" 
    const val UNIT_SETTINGS = "unit_settings_screen" 
    const val WORKOUT_REMINDERS = "workout_reminders_screen" 
    const val DEFAULT_REST_TIME_SETTINGS = "default_rest_time_settings_screen" 
    const val THEME_SETTINGS = "theme_settings_screen" 
    const val CREATE_NEW_LIST = "create_new_list_screen" 
    const val EXERCISE_LIST_DETAIL = "exercise_list_detail_screen/{listId}/{listName}" 
    const val EDIT_LIST_SCREEN = "edit_list_screen/{listId}" // New route for editing a list
    const val FITNESS_MAIN = "fitness_main" // Root for the main app content after auth/onboarding
    const val AD_REWARDS_SCREEN = "ad_rewards_screen" // New route for ad rewards
    const val EXERCISE_DETAIL = "exercise_detail/{exerciseId}" // New route for individual exercise details
}
