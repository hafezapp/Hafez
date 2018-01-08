Feature: Submissions
  In order to submit reports
  As a user
  I want to add text and attachments

  Scenario: Submit a text report
    Given I see the submissions screen
    And   I click the overflow menu
    And   I choose Import File
    Then  success
#
#  Scenario: Submit a media report
#    Given I see the submissions screen
#    And   I click the overflow menu
#    And   I click 'Import File'
#    And   I click 'Submit'
#    Then  I see the contacts page
#
##  Scenario: Start a media report from home screen
##    Given I see the home screen
##    And   I click 'Import File'
##    Then  I see the contacts page
#
#  Scenario: Submit a media report with multiple files
#    Given I see the submissions screen
#    And   I click the overflow menu
#    And   I click 'Import File'
#    And   I choose a file
#    And   I click 'Submit'
#    Then  I see the cases
#
#  Scenario: Add too many files or too large a file
#    Given I see the submissions screen
#    And   I click the overflow menu
#    And   I click 'Import File'
#    And   I choose a file
#    And   I click 'Submit'
#    Then  I see the app settings