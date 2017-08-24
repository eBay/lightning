$("#features").load("features.html");
$("#benchmark").load("benchmark.html");
$("#architecture").load("architecture.html");
$("#getStarted").load("getStarted.html");
$("#modes").load("modes.html");
$("#whenToUse").load("whenToUse.html");
$("#faqs").load("faqs.html");
$("#contribute").load("contribute.html");

$("#sidebar").affix({
    offset: {
      top: 60
    }
});

$(document).ready(function(){
    $("body").scrollspy({target: ".bs-docs-sidebar", offset:10});
});
