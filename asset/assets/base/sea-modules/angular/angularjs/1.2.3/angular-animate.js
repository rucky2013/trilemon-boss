/*
 AngularJS v1.2.3
 (c) 2010-2014 Google, Inc. http://angularjs.org
 License: MIT
*/
define("angular/angularjs/1.2.3/angular-animate",["angularjs"],function(require){ var angular = require("angularjs");(function(C,k,F){'use strict';k.module("ngAnimate",["ng"]).config(["$provide","$animateProvider",function(M,G){var p=k.noop,r=k.forEach,N=G.$$selectors,T=1,h="$$ngAnimateState",H="ng-animate",l={running:!0};M.decorator("$animate",["$delegate","$injector","$sniffer","$rootElement","$timeout","$rootScope","$document",function(v,C,I,g,s,q,F){function O(a){if(a){var d=[],c={};a=a.substr(1).split(".");(I.transitions||I.animations)&&a.push("");for(var e=0;e<a.length;e++){var b=a[e],h=N[b];h&&!c[b]&&(d.push(C.get(h)),
c[b]=!0)}return d}}function m(a,d,c,e,b,l,q){function w(a){t();if(!0===a)u();else{if(a=c.data(h))a.done=u,c.data(h,a);m(x,"after",u)}}function m(e,b,h){var k=b+"End";r(e,function(l,f){var B=function(){a:{var B=b+"Complete",a=e[f];a[B]=!0;(a[k]||p)();for(a=0;a<e.length;a++)if(!e[a][B])break a;h()}};"before"!=b||"enter"!=a&&"move"!=a?l[b]?l[k]=y?l[b](c,d,B):l[b](c,B):B():B()})}function g(){q&&s(q,0,!1)}function t(){t.hasBeenRun||(t.hasBeenRun=!0,l())}function u(){if(!u.hasBeenRun){u.hasBeenRun=!0;var a=
c.data(h);a&&(y?z(c):(a.closeAnimationTimeout=s(function(){z(c)},0,!1),c.data(h,a)));g()}}var k=c.attr("class")||"",v=(" "+(k+" "+d)).replace(/\s+/g,".");e||(e=b?b.parent():c.parent());var v=O(v),y="addClass"==a||"removeClass"==a;b=c.data(h)||{};if(J(c,e)||0===v.length)t(),u();else{var x=[];b.running&&y&&b.structural||r(v,function(b){if(!b.allowCancel||b.allowCancel(c,a,d)){var e=b[a];"leave"==a?(b=e,e=null):b=b["before"+a.charAt(0).toUpperCase()+a.substr(1)];x.push({before:b,after:e})}});0===x.length?
(t(),g()):(e=" "+k+" ",b.running&&(s.cancel(b.closeAnimationTimeout),z(c),L(b.animations),b.beforeComplete?(b.done||p)(!0):y&&!b.structural&&(e="removeClass"==b.event?e.replace(b.className,""):e+b.className+" ")),k=" "+d+" ","addClass"==a&&0<=e.indexOf(k)||"removeClass"==a&&-1==e.indexOf(k)?(t(),g()):(c.addClass(H),c.data(h,{running:!0,event:a,className:d,structural:!y,animations:x,done:w}),m(x,"before",w)))}}function E(a){a=a[0];a.nodeType==T&&r(a.querySelectorAll("."+H),function(a){a=k.element(a);
var c=a.data(h);c&&(L(c.animations),z(a))})}function L(a){r(a,function(d){a.beforeComplete||(d.beforeEnd||p)(!0);a.afterComplete||(d.afterEnd||p)(!0)})}function z(a){a[0]==g[0]?l.disabled||(l.running=!1,l.structural=!1):(a.removeClass(H),a.removeData(h))}function J(a,d){if(l.disabled)return!0;if(a[0]==g[0])return l.disabled||l.running;do{if(0===d.length)break;var c=d[0]==g[0],e=c?l:d.data(h),e=e&&(!!e.disabled||!!e.running);if(c||e)return e;if(c)break}while(d=d.parent());return!0}g.data(h,l);q.$$postDigest(function(){q.$$postDigest(function(){l.running=
!1})});return{enter:function(a,d,c,e){this.enabled(!1,a);v.enter(a,d,c);q.$$postDigest(function(){m("enter","ng-enter",a,d,c,p,e)})},leave:function(a,d){E(a);this.enabled(!1,a);q.$$postDigest(function(){m("leave","ng-leave",a,null,null,function(){v.leave(a)},d)})},move:function(a,d,c,e){E(a);this.enabled(!1,a);v.move(a,d,c);q.$$postDigest(function(){m("move","ng-move",a,d,c,p,e)})},addClass:function(a,d,c){m("addClass",d,a,null,null,function(){v.addClass(a,d)},c)},removeClass:function(a,d,c){m("removeClass",
d,a,null,null,function(){v.removeClass(a,d)},c)},enabled:function(a,d){switch(arguments.length){case 2:if(a)z(d);else{var c=d.data(h)||{};c.disabled=!0;d.data(h,c)}break;case 1:l.disabled=!a;break;default:a=!l.disabled}return!!a}}}]);G.register("",["$window","$sniffer","$timeout",function(l,h,I){function g(f){R.push(f);I.cancel(S);S=I(function(){r(R,function(f){f()});R=[];S=null;D={}},10,!1)}function s(f,a){var K=a?D[a]:null;if(!K){var b=0,c=0,d=0,e=0,h,k,g,m;r(f,function(f){if(f.nodeType==T){f=l.getComputedStyle(f)||
{};g=f[A+G];b=Math.max(q(g),b);m=f[A+t];h=f[A+u];c=Math.max(q(h),c);k=f[w+u];e=Math.max(q(k),e);var a=q(f[w+G]);0<a&&(a*=parseInt(f[w+M],10)||1);d=Math.max(a,d)}});K={total:0,transitionPropertyStyle:m,transitionDurationStyle:g,transitionDelayStyle:h,transitionDelay:c,transitionDuration:b,animationDelayStyle:k,animationDelay:e,animationDuration:d};a&&(D[a]=K)}return K}function q(f){var a=0;f=k.isString(f)?f.split(/\s*,\s*/):[];r(f,function(f){a=Math.max(parseFloat(f)||0,a)});return a}function H(f){var a=
f.parent(),b=a.data(V);b||(a.data(V,++U),b=U);return b+"-"+f[0].className}function O(f,a){var b=H(f),c=b+" "+a,d={},e=D[c]?++D[c].total:0;if(0<e){var h=a+"-stagger",d=b+" "+h;(b=!D[d])&&f.addClass(h);d=s(f,d);b&&f.removeClass(h)}f.addClass(a);c=s(f,c);h=Math.max(c.transitionDuration,c.animationDuration);if(0===h)return f.removeClass(a),!1;var k="";0<c.transitionDuration?(f.addClass(x),k+=N+" ",f[0].style[A+t]="none"):f[0].style[w]="none 0s";r(a.split(" "),function(a,f){k+=(0<f?" ":"")+a+"-active"});
f.data(y,{className:a,activeClassName:k,maxDuration:h,classes:a+" "+k,timings:c,stagger:d,ii:e});return!0}function m(a){a=a[0];var b=A+t;a.style[b]&&0<a.style[b].length&&(a.style[b]="")}function E(a){var b=a[0],c=w;b.style[c]&&0<b.style[c].length&&(a[0].style[c]="")}function L(a,d,e){function k(a){a.stopPropagation();a=a.originalEvent||a;var f=a.$manualTimeStamp||a.timeStamp||Date.now();Math.max(f-w,0)>=v&&a.elapsedTime>=q&&e()}var n=a.data(y);if(a.hasClass(d)&&n){var l=a[0],g=n.timings,m=n.stagger,
q=n.maxDuration,r=n.activeClassName,v=1E3*Math.max(g.transitionDelay,g.animationDelay),w=Date.now(),t=Q+" "+P,u=n.ii,x,n="",p=[];if(0<g.transitionDuration){var s=g.transitionPropertyStyle;-1==s.indexOf("all")&&(x=!0,n+=b+"transition-property: "+s+", "+(h.msie?"-ms-zoom":"border-spacing")+"; ",n+=b+"transition-duration: "+g.transitionDurationStyle+", "+g.transitionDuration+"s; ",p.push(b+"transition-property"),p.push(b+"transition-duration"))}0<u&&(0<m.transitionDelay&&0===m.transitionDuration&&(s=
g.transitionDelayStyle,x&&(s+=", "+g.transitionDelay+"s"),n+=b+"transition-delay: "+z(s,m.transitionDelay,u)+"; ",p.push(b+"transition-delay")),0<m.animationDelay&&0===m.animationDuration&&(n+=b+"animation-delay: "+z(g.animationDelayStyle,m.animationDelay,u)+"; ",p.push(b+"animation-delay")));0<p.length&&(g=l.getAttribute("style")||"",l.setAttribute("style",g+" "+n));a.on(t,k);a.addClass(r);return function(b){a.off(t,k);a.removeClass(r);c(a,d);for(var e in p)l.style.removeProperty(p[e])}}e()}function z(a,
b,c){var d="";r(a.split(","),function(a,f){d+=(0<f?",":"")+(c*b+parseInt(a,10))+"s"});return d}function J(a,b){if(O(a,b))return function(d){d&&c(a,b)}}function a(a,b,d){if(a.data(y))return L(a,b,d);c(a,b);d()}function d(f,b,c){var d=J(f,b);if(d){var e=d;g(function(){m(f);E(f);e=a(f,b,c)});return function(a){(e||p)(a)}}c()}function c(a,b){a.removeClass(b);a.removeClass(x);a.removeData(y)}function e(a,b){var c="";a=k.isArray(a)?a:a.split(/\s+/);r(a,function(a,f){a&&0<a.length&&(c+=(0<f?" ":"")+a+b)});
return c}var b="",A,P,w,Q;C.ontransitionend===F&&C.onwebkittransitionend!==F?(b="-webkit-",A="WebkitTransition",P="webkitTransitionEnd transitionend"):(A="transition",P="transitionend");C.onanimationend===F&&C.onwebkitanimationend!==F?(b="-webkit-",w="WebkitAnimation",Q="webkitAnimationEnd animationend"):(w="animation",Q="animationend");var G="Duration",t="Property",u="Delay",M="IterationCount",V="$$ngAnimateKey",y="$$ngAnimateCSS3Data",x="ng-animate-start",N="ng-animate-active",D={},U=0,R=[],S;return{allowCancel:function(a,
b,c){var d=(a.data(y)||{}).classes;if(!d||0<=["enter","leave","move"].indexOf(b))return!0;var h=a.parent(),g=k.element(a[0].cloneNode());g.attr("style","position:absolute; top:-9999px; left:-9999px");g.removeAttr("id");g.html("");r(d.split(" "),function(a){g.removeClass(a)});g.addClass(e(c,"addClass"==b?"-add":"-remove"));h.append(g);a=s(g);g.remove();return 0<Math.max(a.transitionDuration,a.animationDuration)},enter:function(a,b){return d(a,"ng-enter",b)},leave:function(a,b){return d(a,"ng-leave",
b)},move:function(a,b){return d(a,"ng-move",b)},beforeAddClass:function(a,b,c){if(b=J(a,e(b,"-add")))return g(function(){m(a);E(a);c()}),b;c()},addClass:function(b,c,d){return a(b,e(c,"-add"),d)},beforeRemoveClass:function(a,b,c){if(b=J(a,e(b,"-remove")))return g(function(){m(a);E(a);c()}),b;c()},removeClass:function(b,c,d){return a(b,e(c,"-remove"),d)}}}])}])})(window,angular);return angular.module("ngAnimate");})
//# sourceMappingURL=angular-animate.min.js.map
