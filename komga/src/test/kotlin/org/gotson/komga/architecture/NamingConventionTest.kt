package org.gotson.komga.architecture

import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.gotson.komga.Application
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RestController

@AnalyzeClasses(packagesOf = [Application::class], importOptions = [ImportOption.DoNotIncludeTests::class])

class NamingConventionTest {

  @ArchTest
  val domain_persistence_should_have_names_ending_with_repository: ArchRule =
    classes()
      .that().resideInAPackage("..domain..persistence..")
      .should().haveNameMatching(".*Repository")

  @ArchTest
  val services_should_not_have_names_containing_service_or_manager: ArchRule =
    noClasses()
      .that().resideInAnyPackage("..domain..service..", "..application..service..")
      .should().haveSimpleNameContaining("service")
      .orShould().haveSimpleNameContaining("Service")
      .orShould().haveSimpleNameContaining("manager")
      .orShould().haveSimpleNameContaining("Manager")
      .because("it doesn't bear any intent")

  @ArchTest
  val controllers_should_be_suffixed: ArchRule =
    classes()
      .that().areAnnotatedWith(RestController::class.java)
      .or().areAnnotatedWith(Controller::class.java)
      .should().haveSimpleNameEndingWith("Controller")
}
