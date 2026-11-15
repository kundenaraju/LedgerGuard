# LedgerGuard

LedgerGuard evaluates transaction records against configurable compliance rules. It is a small, runnable Java application intended to demonstrate input validation, clear application/domain/infrastructure boundaries, and automated tests.

## Requirements

- JDK 17 or newer
- Maven 3.8 or newer

## Build, test, and run

```sh
mvn clean verify
java -jar target/ledgerguard-1.0.0.jar
```

By default, the application reads the sample transaction JSON and rules CSV packaged in the jar. To provide different input files:

```sh
java -jar target/ledgerguard-1.0.0.jar --transactions C:\data\transactions.json --rules C:\data\rules.csv
```

Use `--help` to display command-line options. The build creates a JaCoCo coverage report at `target/site/jacoco/index.html`.

## Project structure

```text
src/
  main/
    java/com/ledgerguard/
      LedgerGuardApplication.java       CLI and application composition
      application/ComplianceService.java
      domain/                           Immutable models and rule semantics
      infrastructure/                   JSON and CSV input adapters
    resources/                          Bundled sample inputs
  test/
    java/com/ledgerguard/               Unit and input-reader tests
```

## Input formats

The JSON document contains a `transactions` array. Every transaction requires a unique `transaction_id` and numeric `amount`; other supported fields are `country`, `merchant_category`, `customer_tier`, `currency`, `status`, and an ISO-8601 UTC `timestamp`.

The CSV rules file requires the columns `rule_id`, `rule_name`, `field`, `operator`, `value`, and `severity`. Supported operators are `eq`, `neq`, `gt`, `lt`, and `in`. `gt` and `lt` apply to `amount` and `timestamp.hour`; text fields support `eq`, `neq`, and `in`. `in` values must be quoted in CSV when they contain commas, for example `"US,CA"`. Invalid records and unsupported rules fail with a descriptive error instead of being silently ignored.

## Design notes

- `domain` contains immutable transaction, rule, severity, operator, and breach types.
- `application` evaluates domain objects without depending on file formats or console I/O.
- `infrastructure` adapts JSON and CSV input to validated domain objects.
- The CLI owns startup, argument parsing, resource loading, and presentation.
- The application has no database or network service; the sample data is local and can be replaced via CLI paths.
