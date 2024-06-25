import {ComponentPortal, ComponentType, DomPortalOutlet} from "@angular/cdk/portal";
import {ApplicationRef, ComponentFactoryResolver, Injector} from "@angular/core";
import * as Quill from "quill";

const EmbedBlot = Quill.import("blots/embed");

export function createComponentBlot<T, V>(inj: Injector,
                                          blot: ComponentBlotDefinition<T, V>)
{
    const componentFactoryResolver = inj.get(ComponentFactoryResolver);
    const applicationRef           = inj.get(ApplicationRef);

    class BasicComponentBlot extends EmbedBlot
    {
        static blotName = blot.blotName;
        static tagName  = blot.blotName;

        private portalOutlet: DomPortalOutlet;
        private component: T;

        constructor(node: HTMLSpanElement,
                    value: V)
        {
            super(node, value);

            this.portalOutlet = new DomPortalOutlet(node.querySelector("span"), componentFactoryResolver, applicationRef, inj);
            let portal        = new ComponentPortal(blot.componentType);
            let componentRef  = this.portalOutlet.attachComponentPortal(portal);
            this.component    = componentRef.instance;
            if (blot.initializer) blot.initializer(this.component, value);
        }

        value(): any
        {
            return {[BasicComponentBlot.blotName]: blot.value ? blot.value(this.component) : true};
        }

        remove()
        {
            super.remove();
            this.portalOutlet.dispose();
            this.portalOutlet = null;
            this.component    = null;
        }
    }

    return BasicComponentBlot;
}

export interface ComponentBlotDefinition<T, V>
{
    blotName: string;
    componentType: ComponentType<T>;
    initializer?: (component: T,
                   value: V) => void;
    value?: (component: T) => V;
}

