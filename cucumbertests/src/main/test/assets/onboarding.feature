Feature: Onboarding screen
    In order to complete onboarding
    As a user of the app
    I want to view the onboarding tutorial screens

	Scenario: Viewing first onboarding screen
		Given I see the welcome screen
		And   I click next
		Then  I see the second screen

#	Scenario: Viewing second onboarding screen
#		Given I see the welcome screen
#		And   I click next
		#And   I click next
#		Then  I see the third screen

#	Scenario: Viewing third onboarding screen
#		Given I see the welcome screen
#		And   I click next
#		And   I click next
#		Then  I see the fourth screen
#
#	Scenario: Finishing the tutorial
#		Given I see the welcome screen
#		And   I click next
#		And   I click next
#		And   I click next
#		And	  I click next
#		Then  I finish the activity
