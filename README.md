![Logo][Logo] Chanobol
======================

  [Logo]: https://raw.githubusercontent.com/eugenkiss/chanobol/master/src/main/res/mipmap-mdpi/ic_launcher.png
  [Chanu]: https://github.com/grzegorznittner/chanu
  [U+2020]: https://github.com/JakeWharton/u2020
  [API]: https://github.com/4chan/4chan-API
  [Build Types]: http://tools.android.com/tech-docs/new-build-system/user-guide#TOC-Build-Types
  [Nine Old Androids]: http://nineoldandroids.com/
  [Dagger]: http://square.github.io/dagger/
  [Bindable Adapter]: https://twitter.com/jakewharton/status/325368867109076993
  [Ion]: https://github.com/koush/ion/
  [Support]: http://developer.android.com/tools/support-library/index.html
  [RecyclerView]: https://developer.android.com/reference/android/support/v7/widget/RecyclerView.html
  [RecyclerView]: https://developer.android.com/reference/android/support/v7/widget/RecyclerView.html
  [iosched]: https://github.com/google/iosched

Chanobol is a 4Chan reader app on steroids\* inspired by [Chanu][].
Get in on the [Play Store](https://play.google.com/store/apps/details?id=anabolicandroids.chanobol.sfw).

<sup>\*Steroids being the employed libraries like Dagger and Ion.</sup>


Status
------

Chanobol is work in progress and currently offers a read-only 4Chan browsing
experience. I am very happy to accept pull requests for new features or
improvements! I have already created a bunch of issues which you could take as a
starting point.

<sub>
FYI: Basically, I wanted to create an optimized Chanu for myself with Chanobol.
As I used only a subset of Chanu's functionality I actually do not really care
about Chanobol gaining features beyond this subset like for example posting
replies or choosable themes. My aim was and is to have a polished read-only
4Chan experience. That does not mean I would reject pull requests for these
features. It only means that I probably will not implement them myself.
</sub>

Mockmode currently is not really working, see issues
[#34](https://github.com/eugenkiss/chanobol/issues/34) and
[#47](https://github.com/eugenkiss/chanobol/issues/34).

If you clone this project and want to build/run it you will need to comment
some lines in `build.gradle`. `build.gradle` contains information about
the offending lines. Sadly, I could not find a better way.


Motivation
----------

First and foremost Chanobol is a learning project for me to get more
familiar with Android development. A 4Chan app seemed like a fruitful
undertaking in that respect.

Second, I wanted to create an optimized Chanu for my very own user behavior.
Although I find Chanu great there are a couple of tiny things I do not like
about its UI:

* There is no up button on the thread view in Chanu. This turned out
  to be problematic for me due to the way I was holding my Nexus 7.
  Chanobol has an up button on the thread view which makes it easier
  for me to return to the catalog view.
* Chanu apparently has this automatic refresh feature on the catalog
  view where it refreshes the catalog periodically which leads to
  a reordering of the catalog entries. It is a bit aggravating for me
  because I did not initiate this reordering and a thread I want to
  click on may suddenly disappear and reappear in a different position.
  Chanobol still has an automatic refresh feature. However, it does not
  reorder the catalog entries. This only happens on a manual refresh.
* The replies dialog, i.e. the one where all replies to a specific post
  can be read, is not easily dismissable from the top of the screen in Chanu if
  the dialog fills the screen. In Chanobol, when the replies dialog is shown,
  the toolbar remains visible and has an up button. Furthermore, Chanobol's UI
  for viewing replies is different in that it is possible to have a stack of
  replies dialogs whose entries can be dismissed individually or all at once.

Why did I open source Chanobol?

* *To learn from others*. I hope that people will solve some
  of the problems in the code or from the issues where I did not find a good solution
  and thus I will learn how to solve such a problem in the future.
* *To get help*. I hope that people will improve things in areas
  where I am not strong like, for instance, the (graphical) design of the
  application.
* *To share*. Maybe people will find this project useful as it
  is, among other things, a non-demo application of concepts from [U+2020][].


Code Architecture
-----------------

This section gives a broad overview of the code, some (non-obvious) code design
decisions and the reasons for the application of some libraries. There are many
more decisions, of course, which can be found by studying the code or the [FAQ
in the wiki](https://github.com/eugenkiss/chanobol/wiki#faq). I tried to follow
good practices to the best of my knowledge. As pure Android development without
the help of libraries is often relatively tedious I employ useful libraries
where possible.

The high-level code structure is very much inspired by [U+2020][]. There is a
debug and a release [build type][Build Types]. The debug build type has a mock
mode just like [U+2020][] where server requests are mocked to return data from
the debug build type's assets folder. Instead of a debug drawer like in
[U+2020][] Chanobol employs a debug preference activity which can be openend
from the drawer. Dependencies are injected with [Dagger][]. Chanobol has an
`AppModule` which contains application-level or rather global dependenices and
an `ApiModule` for server-related dependencies. The debug build type overwrites
some module definitions in `DebugAppModule`. Where it makes sense
[`RecyclerView`][RecyclerView] is used in place of `ListView` or `GridView`
which has several advantages like better scrolling performance and more precise
scrolling information. In the vein of [The Google I/O 2014 Android App][iosched]
Chanobol employs an activity-based drawer layout. With [Ion][] 4Chan's [API][]
is mapped to the interface `ChanService` in the `api` package. On top of that,
Ion takes care of loading and caching images.

<sub>
A constraint I set is that Chanobol must be runnable on API Level 9 to see what
kind of workarounds are needed and what pitfalls there are for developing an
Android application that is backwards compatible. That is the reason for the
[`com.android.support:`*X*][Support]-dependencies as well as the dependency on
[Nine Old Androids][].
</sub>
