[![CircleCI](https://circleci.com/gh/navikt/helse-spenn/tree/master.svg?style=svg)](https://circleci.com/gh/navikt/helse-spenn/tree/master)
[![Known Vulnerabilities](https://snyk.io/test/github/navikt/helse-spenn/badge.svg)](https://snyk.io/test/github/navikt/helse-spenn)

## Spenn 

Depends on: 
  * kafka
  * IBM MQ
  * Aktørregisteret
  * postgres
  * vault


### Running in dev or localhost

```
mvn clean install
docker-compose up 
```