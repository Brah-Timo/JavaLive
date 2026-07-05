package com.example.components;

import io.javalive.annotations.*;
import org.springframework.stereotype.Controller;

/**
 * The simplest possible JavaLive component — a reactive counter widget.
 *
 * Demonstrates:
 * - @VueComponent (widget, not a full page)
 * - @Reactive (session-scoped integer)
 * - @VueMethod (server-side methods called from template)
 * - @VueComputed (derived values)
 * - @VueTemplate (inline template)
 */
@Controller
@VueComponent("counter-widget")
public class CounterWidget {

    @Reactive
    public int count = 0;

    @Reactive
    public int step = 1;

    // ── Methods ───────────────────────────────────────────────────────────────

    @VueMethod
    public void increment() {
        this.count += this.step;
    }

    @VueMethod
    public void decrement() {
        this.count -= this.step;
    }

    @VueMethod
    public void reset() {
        this.count = 0;
    }

    @VueMethod
    public void setStep(int newStep) {
        if (newStep > 0) this.step = newStep;
    }

    // ── Computed ──────────────────────────────────────────────────────────────

    @VueComputed
    public boolean isZero() {
        return this.count == 0;
    }

    @VueComputed
    public boolean isNegative() {
        return this.count < 0;
    }

    // ── Template ──────────────────────────────────────────────────────────────

    @VueTemplate
    static final String template = """
        <div class="counter-widget">
            <div class="counter-display" :class="{ 'text-red-500': isNegative, 'text-gray-400': isZero }">
                {{ state.count }}
            </div>
            <div class="counter-controls">
                <button @click="decrement" class="btn btn-outline">−</button>
                <button @click="reset" :disabled="isZero" class="btn btn-ghost">Reset</button>
                <button @click="increment" class="btn btn-primary">+</button>
            </div>
            <div class="step-control">
                <label>Step:</label>
                <input
                    type="number"
                    :value="state.step"
                    @change="setStep(Number($event.target.value))"
                    min="1" max="100"
                    class="input input-sm w-20"
                />
            </div>
            <div v-if="!isConnected" class="text-yellow-500 text-sm">
                ⚠ Reconnecting...
            </div>
        </div>
    """;
}
