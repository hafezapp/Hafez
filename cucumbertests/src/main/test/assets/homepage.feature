Feature: Homepage
    In order to browse the app
    As a new user
    I want to check out all the available activities

    Scenario: Check out the newsfeed
        Given I see the home screen
        And   I click 'News'
        Then  I see the newsfeed

    Scenario: Check out the contacts
        Given I see the home screen
        And   I click 'Contacts'
        Then  I see the contacts page

    Scenario: Check out the cases
        Given I see the home screen
        And   I click 'Cases'
        Then  I see the cases

    Scenario: See the app settings
        Given I see the home screen
        And   I click the overflow menu
        And   I click 'Settings'
        Then  I see the app settings