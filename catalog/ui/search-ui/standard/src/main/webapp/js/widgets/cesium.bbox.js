/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
/*global define*/

define([
        'marionette',
        'backbone',
        'cesium',
        'underscore',
        'wreqr',
        'maptype',
        './notification.view'
    ],
    function (Marionette, Backbone, Cesium, _, wreqr, maptype, NotificationView) {
        "use strict";
        var Draw = {};

        Draw.BboxModel = Backbone.Model.extend({
            defaults: {
                north: undefined,
                east: undefined,
                west: undefined,
                south: undefined
            }
        });
        var defaultAttrs = ['north', 'east', 'west', 'south'];
        Draw.BboxView = Backbone.View.extend({
            initialize: function (options) {
                this.canvas = options.scene.canvas;
                this.scene = options.scene;
                this.ellipsoid = options.scene.globe.ellipsoid;
                this.mouseHandler = new Cesium.ScreenSpaceEventHandler(this.canvas);
                this.listenTo(this.model, 'change:north change:south change:east change:west', this.updatePrimitive);
            },
            enableInput: function () {
                var controller = this.scene.screenSpaceCameraController;
                controller.enableTranslate = true;
                controller.enableZoom = true;
                controller.enableRotate = true;
                controller.enableTilt = true;
                controller.enableLook = true;
            },
            disableInput: function () {
                var controller = this.scene.screenSpaceCameraController;
                controller.enableTranslate = false;
                controller.enableZoom = false;
                controller.enableRotate = false;
                controller.enableTilt = false;
                controller.enableLook = false;
            },
            setModelFromClicks: function (mn, mx) {

                var e = new Cesium.Rectangle(),
                    epsilon = Cesium.Math.EPSILON7,
                    modelProps;


                if (!this.lastLongitude) {
                    this.crossDateLine = false;
                    this.lastLongitude = mx.longitude;
                } else {
                    if (this.lastLongitude > 0 && mx.longitude > 0 && mn.longitude > 0) {
                        //west of the date line
                        this.crossDateLine = false;
                        //track direction of the bbox
                        if (this.lastLongitude > mx.longitude) {
                            if (this.dir === "east") {
                                if (mx.longitude < mn.longitude) {
                                    this.dir = "west";
                                }
                            } else {
                                this.dir = "west";
                            }
                        } else if (this.lastLongitude < mx.longitude) {
                            if (this.dir === "west") {
                                if (mx.longitude > mn.longitude) {
                                    this.dir = "east";
                                }
                            } else {
                                this.dir = "east";
                            }
                        }
                    } else if (this.lastLongitude > 0 && mx.longitude < 0 && mn.longitude > 0) {
                        //crossed date line from west to east
                        this.crossDateLine = !(this.dir && this.dir === "west");
                    } else if (this.lastLongitude < 0 && mx.longitude > 0 && mn.longitude > 0) {
                        //moved back across date line to same quadrant
                        this.crossDateLine = false;
                    } else if (this.lastLongitude < 0 && mx.longitude < 0 && mn.longitude < 0) {
                        //east of the date line
                        this.crossDateLine = false;
                        //track direction of the bbox
                        if (this.lastLongitude < mx.longitude) {
                            if (this.dir === "west") {
                                if (mx.longitude > mn.longitude) {
                                    this.dir = "east";
                                }
                            } else {
                                this.dir = "east";
                            }
                        } else if (this.lastLongitude > mx.longitude) {
                            if (this.dir === "east") {
                                if (mx.longitude < mn.longitude) {
                                    this.dir = "west";
                                }
                            } else {
                                this.dir = "west";
                            }
                        }
                    } else if (this.lastLongitude < 0 && mx.longitude > 0 && mn.longitude < 0) {
                        //crossed date line from east to west
                        this.crossDateLine = !(this.dir && this.dir === "east");
                    } else if (this.lastLongitude > 0 && mx.longitude < 0 && mn.longitude < 0) {
                        //moved back across date line to same quadrant
                        this.crossDateLine = false;
                    }
                    this.lastLongitude = mx.longitude;
                }

                // Re-order so west < east and south < north
                if (this.crossDateLine) {
                    e.east = Math.min(mn.longitude, mx.longitude);
                    e.west = Math.max(mn.longitude, mx.longitude);
                } else {
                    e.east = Math.max(mn.longitude, mx.longitude);
                    e.west = Math.min(mn.longitude, mx.longitude);
                }
                e.south = Math.min(mn.latitude, mx.latitude);
                e.north = Math.max(mn.latitude, mx.latitude);

                // Check for approx equal (shouldn't require abs due to
                // re-order)

                if ((e.east - e.west) < epsilon) {
                    e.east += epsilon * 2.0;
                }

                if ((e.north - e.south) < epsilon) {
                    e.north += epsilon * 2.0;
                }

                modelProps = _.pick(e, 'north', 'east', 'west', 'south');
                _.each(modelProps, function (val, key) {
                    modelProps[key] = (val * 180 / Math.PI).toFixed(4);
                });
                this.model.set(modelProps);

                return e;
            },

            modelToRectangle: function (model) {
                var toRad = Cesium.Math.toRadians;
                var obj = model.toJSON();
                if (_.every(defaultAttrs, function (val) {
                    return _.isUndefined(obj[val]);
                }) || _.isEmpty(obj)) {
                    if(this.scene && this.scene.primitives) {
                        this.scene.primitives.remove(this.primitive);
                    }
                    this.stopListening();
                    return;
                }
                _.each(obj, function (val, key) {
                    obj[key] = toRad(val);
                });
                var rectangle = new Cesium.Rectangle();
                if (!obj.north || isNaN(obj.north) || !obj.south || isNaN(obj.south) || !obj.east || isNaN(obj.east) || !obj.west || isNaN(obj.west)) {
                    return null;
                }

                rectangle.north = obj.north;
                rectangle.south = obj.south;
                rectangle.east = obj.east;
                rectangle.west = obj.west;
                return rectangle;
            },

            updatePrimitive: function (model) {
                var rectangle = this.modelToRectangle(model);
                // make sure the current model has width and height before drawing
                if (rectangle && !_.isUndefined(rectangle) && (rectangle.north !== rectangle.south && rectangle.east !== rectangle.west)) {
                    this.drawBorderedRectangle(rectangle);
                    //only call this if the mouse button isn't pressed, if we try to draw the border while someone is dragging
                    //the filled in shape won't show up
                    if (!this.buttonPressed) {
                        this.drawBorderedRectangle(rectangle);
                    }
                }
            },

            updateGeometry: function (model) {
                var rectangle = this.modelToRectangle(model);
                if (rectangle && !_.isUndefined(rectangle) && (rectangle.north !== rectangle.south && rectangle.east !== rectangle.west)) {
                    this.drawBorderedRectangle(rectangle);
                }
            },

            drawBorderedRectangle: function (rectangle) {

                if (!rectangle) {
                    // handles case where model changes to empty vars and we don't want to draw anymore
                    return;
                }

                // first destroy old one
                if (this.primitive && !this.primitive.isDestroyed()) {
                    this.scene.primitives.remove(this.primitive);
                }

                this.primitive = new Cesium.Primitive({
                    asynchronous: false,
                    geometryInstances: [new Cesium.GeometryInstance({
                        geometry: new Cesium.RectangleOutlineGeometry({
                            rectangle: rectangle
                        }),
                        attributes: {
                            color: Cesium.ColorGeometryInstanceAttribute.fromColor(Cesium.Color.KHAKI)
                        }
                    })],
                    appearance: new Cesium.PerInstanceColorAppearance({
                        flat: true,
                        renderState: {
                            depthTest: {
                                enabled: true
                            },
                            lineWidth: Math.min(4.0, this.scene.context.maximumAliasedLineWidth)
                        }
                    })
                });

                this.scene.primitives.add(this.primitive);
            },

            handleRegionStop: function () {
                this.enableInput();
                this.mouseHandler.destroy();
                this.drawBorderedRectangle(this.primitive.rectangle);
                this.stopListening(this.model, 'change:north change:south change:east change:west', this.updatePrimitive);
                this.listenTo(this.model, 'change:north change:south change:east change:west', this.updateGeometry);

                this.model.trigger("EndExtent", this.model);
                this.dir = undefined;
                this.lastLongitude = undefined;
                this.crossDateLine = undefined;
            },
            handleRegionInter: function (movement) {
                var cartesian = this.scene.camera
                    .pickEllipsoid(movement.endPosition, this.ellipsoid), cartographic;
                if (cartesian) {
                    cartographic = this.ellipsoid.cartesianToCartographic(cartesian);
                    this.setModelFromClicks(this.click1, cartographic);
                }
            },
            handleRegionStart: function (movement) {
                var cartesian = this.scene.camera
                    .pickEllipsoid(movement.position, this.ellipsoid), that = this;
                if (cartesian) {
                    // var that = this;
                    this.click1 = this.ellipsoid
                        .cartesianToCartographic(cartesian);
                    this.mouseHandler.setInputAction(function () {
                        that.buttonPressed = false;
                        that.handleRegionStop();
                    }, Cesium.ScreenSpaceEventType.LEFT_UP);
                    this.mouseHandler.setInputAction(function (movement) {
                        that.buttonPressed = true;
                        that.handleRegionInter(movement);
                    }, Cesium.ScreenSpaceEventType.MOUSE_MOVE);
                }
            },
            start: function () {
                this.disableInput();

                var that = this;

                // Now wait for start
                this.mouseHandler.setInputAction(function (movement) {
                    that.handleRegionStart(movement);
                }, Cesium.ScreenSpaceEventType.LEFT_DOWN);
            },


            stop: function () {
                this.stopListening();
                this.enableInput();

            },


            destroyPrimitive: function () {
                if (!this.mouseHandler.isDestroyed()) {
                    this.mouseHandler.destroy();
                }
                if (this.primitive && !this.primitive.isDestroyed()) {
                    this.scene.primitives.remove(this.primitive);
                }
            }

        });

        Draw.Controller = Marionette.Controller.extend({
            enabled: maptype.is3d(),
            initialize: function (options) {
                this.scene = options.scene;
                this.notificationEl = options.notificationEl;

                this.listenTo(wreqr.vent, 'search:bboxdisplay', this.showBox);
                this.listenTo(wreqr.vent, 'search:drawbbox', this.draw);
                this.listenTo(wreqr.vent, 'search:drawstop', this.stop);
                this.listenTo(wreqr.vent, 'search:drawend', this.destroy);
            },
            showBox: function(model) {
                if (this.enabled) {
                    var bboxModel = model || new Draw.BboxModel(),
                        view = new Draw.BboxView(
                            {
                                scene: this.scene,
                                model: bboxModel
                            });

                    if (this.view) {
                        this.view.destroyPrimitive();
                        this.view.stop();

                    }
                    view.updatePrimitive(model);
                    this.view = view;

                    return bboxModel;
                }
            },
            draw: function (model) {
                if (this.enabled) {
                    var bboxModel = model || new Draw.BboxModel(),
                        view = new Draw.BboxView(
                            {
                                scene: this.scene,
                                model: bboxModel
                            });

                    if (this.view) {
                        this.view.destroyPrimitive();
                        this.view.stop();

                    }
                    view.start();
                    this.view = view;
                    this.notificationView = new NotificationView({
                        el: this.notificationEl
                    }).render();
                    bboxModel.trigger('BeginExtent');
                    this.listenToOnce(bboxModel, 'EndExtent', function () {
                        this.notificationView.destroy();
                    });

                    return bboxModel;
                }
            },
            stop: function () {
                if (this.enabled && this.view) {
                    this.view.stop();
                    if(this.notificationView) {
                        this.notificationView.destroy();
                    }
                }
            },
            destroy: function () {
                if (this.enabled && this.view) {
                    this.view.stop();
                    this.view.destroyPrimitive();
                    this.view = undefined;
                    if(this.notificationView) {
                        this.notificationView.destroy();
                    }
                }
            }
        });

        return Draw;
    });