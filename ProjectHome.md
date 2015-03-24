Aimed for Polish developers (since it uses official Polish exchange provider) command line utility that process income reports from Google Play Store and generates Excel document with transaction list, pivot table with per-day per-currency income and quote difference between official NBP exchange quotes and one used by Google. It also collects VAT related information and other data.

Data can be provided locally or fetched directly from Google Cloud Storage bucket through Google GCS API (either using user credential through OAuth2 or using authorized service, see below for details).




### Usage (assuming application was compiled to runnable jar named isp.jar) ###

`java -jar isp.jar [OPTIONS]...`


### Options ###

<dl>
<dt>-h,--help</dt>
<dd>extended help</dd>
<dt>-o,--output <code>&lt;arg&gt;</code></dt>
<dd>output file name (optional, by default yyyyMM.xlsx, where date is report date</dd>
<dt>--no-overwrite</dt>
<dd>prevents output file overwrite</dd>
<dt>-C,--config <code>&lt;arg&gt;</code></dt>
<dd>properties file with application configuration (use -h for config template)</dd>
<dt>-D,--date <code>&lt;arg&gt;</code></dt>
<dd>raport date in format yyyy.MM or yyyyMM, only used if local-dir is not defined (optional, by default previous month is used)</dd>
<dt>-L,--local-dir <code>&lt;arg&gt;</code></dt>
<dd>use local files from specified directory instead of GCS</dd>
<dt>--gcs-bucket <code>&lt;arg&gt;</code></dt>
<dd>GCS bucked for Play reports</dd>
<dt>--gcs-client-secret <code>&lt;arg&gt;</code></dt>
<dd>Google API client secret json file</dd>
<dt>--gcs-service-cert <code>&lt;arg&gt;</code></dt>
<dd>Google API service account access cert PK12 file</dd>
<dt>--gcs-service-email <code>&lt;arg&gt;</code></dt>
<dd>Google API service account e-mail address</dd>
<dt>--no-vat</dt>
<dd>disables VAT data processing (sales reports will not be used, implies no-vat-sheet)</dd>
<dt>--process-tax-reports</dt>
<dd>enables processing of tax deduction report, currently there are two reports generated per month, transactions and tax deduction (for Brazil, possibly other countries), yet this data seems to be included in main report as well hence is ignored by default</dd>
<dt>--no-summary-sheet</dt>
<dd>disables summary sheet output</dd>
<dt>--no-vat-sheet</dt>
<dd>disables vat sheet output</dd>
<dt>--no-xchange-sheet</dt>
<dd>disables xchange sheet output (no xchange data will be fetched)</dd>
<dt>-v,--verbose</dt><dd>verbose debug messages</dd>
</dl>


### GCS configration ###

To use GCS fetch Google API access has to be configured, there are two possible method:
  * User credentials using oauth2 and client secret - to do so client secret json file has to be provided (see here form more information: [setting oauth2 for Google API](https://developers.google.com/console/help/new/#generatingoauth2)). This method requires manual operation to obtain access token (Google account login page will be opened in default browser).
  * Service access - with private key and service e-mail (see here for more info: [setting service account for Google API](https://developers.google.com/console/help/new/#serviceaccounts)). This is fully automated (ie. no need for manual login) but generated service e-mail address has to be authorized in Google Play developer console for financial data access