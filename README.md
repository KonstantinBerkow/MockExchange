## Description
App simulates exchnaging currencies between your accounts.
Everything happens locally.
Exchange rates are fetced from remote source with defined timer.

## Building
To build app you'll have to add 'api_url' property to your project's 'local.properties'.
It should look roughly like this:
```
sdk.dir=C\:\\Users\\...\\AppData\\Local\\Android\\Sdk
api_url=https://[REDACTED]/[PURGED]/api/
```
Without 'api_url' app won't assemble. Without valid url link you won't get real time exchange updates (and possibly crash).

## Business rules implementation
All business rules (account checking, withdrawal/transfer) are simulated locally in memory valid while app is alive.
So after systems kills app your previous actions are forgotten.

## Balances
Initially only balance in EUR is visible, after trading EUR for something else you'll see new balance.

## Operation precision
For ease of an implementation financial operations are performed in cents. Funds are reprersented by UInt (unsigned int 32).
Some places (user input) lack proper input validation (can overflow).
