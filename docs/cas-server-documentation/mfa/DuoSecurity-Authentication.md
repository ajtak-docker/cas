---
layout: default
title: CAS - Duo Security Authentication
category: Multifactor Authentication
---

{% include variables.html %}

# Duo Security Authentication

[Duo Security](https://www.duo.com) is a two-step verification service the provides 
additional security for access to institutional and personal data.  

Duo offers several options for authenticating users:

- a mobile push notification and one-button verification of identity to a smartphone (requires the free Duo Mobile app)
- a one-time code generated on a smartphone
- a one-time code generated by Duo and sent to a handset via SMS text messaging
- a telephone call from that will prompt you to validate the login request

{% include_cached casmodule.html group="org.apereo.cas" module="cas-server-support-duo" %}

<div class="alert alert-warning">:warning: <strong>Usage</strong>
<p>Please note that support for Duo multifactor authentication that is based on the Duo's Web SDK and the embedded iFrame
is deprecated and scheduled to be removed on March 30, 2024. You should consider switching to the 'Universal Prompt' variant
described in this document to avoid surprises in future upgrades.</p>
</div>

## Actuator Endpoints
      
The following endpoints are provided by CAS:

{% include_cached actuators.html endpoints="duoPing,duoAccountStatus,health,duoAdmin" healthIndicators="duoSecurityHealthIndicator" %}

## Multiple Instances

CAS multifactor authentication support for Duo Security allows
multiple Duo providers to be configured with distinct ids each of
which may be connected to a separate Duo Security instance with a different configuration.
This behavior allows more sensitive applications to be connected
to a Duo instance that has more strict and secure authentication policies.

For this behavior to function, separate unique ids of your own choosing need to be assigned to each Duo Security
provider. Each provider instance is registered with CAS and activated in the authentication
flows as necessary. The provider id need not be defined if there is only a single Duo instance available.

## Account Profile Management

The integration with Duo Security is able to provide user device registration information to the account profile management feature in CAS. [See this guide](../registration/Account-Management-Overview.html) for better details.

## User Account Status

If users are unregistered with Duo Security or allowed through via a direct bypass, 
CAS will query Duo Security for the user account apriori to learn
whether user is registered or configured for direct bypass. If the account is configured for direct bypass or the
user account is not registered yet the new-user enrollment policy allows the user to skip registration, CAS will bypass
Duo Security altogether and shall not challenge the user and will also **NOT** report back a multifactor-enabled 
authentication context back to the application.

<div class="alert alert-warning">:warning: <strong>YMMV</strong><p>In recent conversations with Duo Security, it 
turns out that the API behavior has changed (for security reasons) where it may no longer accurately 
report back account status. This means even if the above conditions hold true, CAS may continue to route 
the user to Duo Security having received an eligibility status from the API. Duo Security is reportedly 
working on a fix to restore the API behavior in a more secure way. In the meanwhile, YMMV.</p></div>

## Health Status

CAS is able to contact Duo Security, on demand, in order to inquire 
the health status of the service using Duo Security's `ping` API. 
The results of the operations are recorded and reported using `health` endpoint 
provided by [CAS Monitoring endpoints](../monitoring/Monitoring-Statistics.html).
Of course, the same result throughout the Duo authentication flow is also used to determine failure modes.
  
## User Registration

If you would rather not rely on Duo Security's built-in registration flow and have your
own registration application that allows users to onboard and enroll with Duo Security, you can instruct CAS
to redirect to your enrollment application, if the user's account status is determined to require enrollment.
This typically means that you must turn on user-account-status checking in CAS so that it can verify
the user's account status directly with Duo Security. You must also make sure your integration type, as selected
in Duo Security's admin dashboard, is chosen to be the correct type that would allow CAS to execute such
requests and of course, the user in question must not have been onboard, enrolled or created previously anywhere
in Duo Security. 
                   
The redirect URL to your enrollment application may include a special `principal` parameter that contains
the user's identity as JWT. Cipher operations and settings must be enabled in CAS settings for Duo Security's
registration before this parameter can be built and added to the final URL.

{% include_cached casproperties.html properties="cas.authn.mfa.duo[].registration" %}

## Universal Prompt

Universal Prompt is a variation of Duo Multifactor Authentication 
that uses the [Duo OIDC Auth API](https://duo.com/docs/oauthapi). This is 
an OIDC standards-based API for adding strong two-factor authentication 
to CAS. This option no longer displays the Duo Prompt 
in an iFrame controlled and owned by CAS. Rather, the prompt is now 
hosted on Duo’s servers and displayed via browser redirects. The
response from Duo Security is passed to CAS as a browser redirect 
and CAS will begin to negotiate and exchange that response in favor of
a JWT that contains the multifactor authentication user profile details.

Universal Prompt no longer requires you to generate and use a application
key value. Instead, it requires a *client id* and *client secret*, which
are known and taught CAS using the integration key and secret key
configuration settings. You will need get your integration key, secret key, and API
hostname from Duo Security when you register CAS as a protected application.
 
## Non-Browser MFA

The Duo Security module of CAS is able to also support [non-browser based multifactor authentication](https://duo.com/docs/authapi) requests.
In order to trigger this behavior, applications (i.e. `curl`, REST APIs, etc) need to specify a special
`Content-Type` to signal to CAS that the request is submitted from a non-web based environment. 
The multifactor authentication request is [submitted to Duo Security](https://duo.com/docs/authapi#/auth) in `auto` mode which effectively may 
translate into an out-of-band factor (push or phone) recommended by Duo as the best for the user's devices.

In order to successfully complete the authentication flow, CAS must also be configured with a method
of primary authentication that is able to support non-web based environments 
such as [Basic Authentication](../authentication/Basic-Authentication.html).

Here is an example using `curl` that attempts to authenticate into a service by first exercising
basic authentication while identifying the request content type as `application/cas`. It is assumed that the
service below is configured in CAS with a special multifactor policy that forces the flow
to pass through Duo Security as well.

```bash
curl --location --header "Content-Type: application/cas" https://apps.example.org/myapp -L -u casuser:Mellon
```

## Configuration

{% include_cached casproperties.html properties="cas.authn.mfa.duo" %}

## REST Protocol Credential Extraction

In the event that the [CAS REST Protocol](../protocol/REST-Protocol.html) is turned on, a 
special credential extractor is injected into the REST authentication engine in order 
to recognize credentials and authenticate them as part of the REST request.
The expected parameter name in the request body is `passcode` that can be found from
Duo Security's mobile application or received via SMS.

## Troubleshooting

To enable additional logging, configure the log4j configuration file to add the following
levels:

```xml
...
<Logger name="com.duosecurity" level="debug" additivity="false">
    <AppenderRef ref="console"/>
    <AppenderRef ref="file"/>
</Logger>
...
``` 

You should also use NTP to ensure that your CAS server's time is correct. Furthermore, CAS typically communicates with 
Duo's service on TCP port 443. Firewall configurations that restrict outbound access to 
Duo's service with rules using destination IP addresses or IP address ranges are not recommended per Duo Security, 
since these may change over time to maintain our service's high availability.
