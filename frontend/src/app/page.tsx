import type { Metadata } from "next";
import { Suspense } from "react";
import { LandingHero } from "@/features/landing/landing-hero";
import {
  LandingClosing,
  LandingFaq,
  LandingFeatures,
  LandingFooter,
  LandingHeader,
  LandingHowItWorks,
  LandingPerks,
  LandingPlans,
  LandingSegments,
  PlansHeading,
} from "@/features/landing/landing-sections";
import { PlanList } from "@/features/plans/plan-list";
import { messages } from "@/messages/pt-BR";

export const metadata: Metadata = {
  title: messages.app.title,
};

/** A página inicial (ohchefe.app.br): apresenta o produto e leva ao teste grátis. */
export default function HomePage() {
  return (
    <div className="flex flex-col">
      <LandingHeader />
      <main>
        <LandingHero />
        <LandingSegments />
        <LandingHowItWorks />
        <LandingFeatures />
        <LandingPerks />
        <LandingPlans>
          <Suspense
            fallback={
              <>
                <PlansHeading />
                <p className="mt-10 text-muted-foreground">{messages.home.plansLoading}</p>
              </>
            }
          >
            <PlanList heading={<PlansHeading />} />
          </Suspense>
        </LandingPlans>
        <LandingFaq />
        <LandingClosing />
      </main>
      <LandingFooter />
    </div>
  );
}
