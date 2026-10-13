import { Link } from 'react-router-dom'
import { FadeIn, Stagger } from '../components/motion'
import { formatCurrency } from '../utils/format'
import Brand from '../components/Brand'

const businessTypes = [
  {
    icon: '☕',
    title: 'Restaurant / Cafe',
    description: 'Keep service moving and every table in view.',
    features: ['Track tables and orders', 'Manage your menu and availability', 'Bill customers in a few taps'],
  },
  {
    icon: '▤',
    title: 'Retail shop',
    description: 'Know what is selling and what needs restocking.',
    features: ['Find products and check stock', 'Record sales at the counter', 'See low-stock items early'],
  },
]

const steps = [
  { number: '01', title: 'Create your workspace', description: 'Add your business details and choose the tools that fit your work.' },
  { number: '02', title: 'Bring your team in', description: 'Set up your products or menu and give staff the access they need.' },
  { number: '03', title: 'Run your day', description: 'Manage orders, billing, stock and sales from one clear home.' },
]

const features = [
  { icon: '₹', title: 'Simple billing', description: 'Create clear bills and record payments without slowing down service.' },
  { icon: '▤', title: 'Stock at a glance', description: 'See what you have on hand and spot items that need attention.' },
  { icon: '♙', title: 'Team access', description: 'Give each staff member the right access for their role.' },
  { icon: '◷', title: 'Daily sales', description: 'Understand how your business is doing with easy-to-read summaries.' },
  { icon: '☷', title: 'Menu and products', description: 'Keep your dishes, products and prices organized in one place.' },
  { icon: '◈', title: 'One business view', description: 'Bring everyday work together in a workspace made for your business.' },
]

function DashboardPreview() {
  const bars = [34, 51, 43, 65, 48, 76, 57]

  return (
    <div className="landing-preview" aria-label="Example business dashboard showing sales in Indian rupees">
      <div className="landing-preview__top">
        <span className="landing-preview__mark">O</span>
        <span><strong>Good morning, Alex</strong><small>Your business at a glance</small></span>
        <span className="landing-preview__live"><i /> Today</span>
      </div>
      <div className="landing-preview__stats">
        <div><span>Today’s sales</span><strong>{formatCurrency(24580)}</strong><small>↑ 12.8% this week</small></div>
        <div><span>Orders</span><strong>48</strong><small>Ready to keep moving</small></div>
      </div>
      <div className="landing-preview__chart">
        <div className="landing-preview__chart-heading"><span>Sales this week</span><strong>{formatCurrency(8429)}</strong></div>
        <div className="landing-preview__bars" aria-hidden="true">
          {bars.map((height, index) => <span key={index} style={{ height: `${height}%` }} />)}
        </div>
        <div className="landing-preview__days"><span>Mon</span><span>Tue</span><span>Wed</span><span>Thu</span><span>Fri</span><span>Sat</span><span>Sun</span></div>
      </div>
      <div className="landing-preview__footer"><span className="landing-preview__check">✓</span><span>Everything you need, in one place</span></div>
    </div>
  )
}

export default function LandingPage() {
  return (
    <main className="landing-v2">
      <header className="landing-v2__header">
        <div className="landing-v2__nav">
          <Brand />
          <nav className="landing-v2__links" aria-label="Main navigation">
            <a href="#features">Features</a>
            <a href="#business-types">Business types</a>
          </nav>
          <div className="landing-v2__actions">
            <Link className="landing-v2__signin" to="/login">Sign in</Link>
            <Link className="ui-button ui-button--primary" to="/register">Start free <span aria-hidden="true">→</span></Link>
          </div>
        </div>
      </header>

      <section className="landing-v2__hero">
        <div className="landing-v2__hero-inner">
          <FadeIn className="landing-v2__hero-copy">
            <span className="landing-v2__eyebrow"><span /> BUSINESS, WITHOUT THE BUSYWORK</span>
            <h1>Manage your restaurant or shop in one simple app</h1>
            <p>Billing, stock, staff and daily sales. No technical knowledge needed.</p>
            <div className="landing-v2__hero-actions">
              <Link className="ui-button ui-button--primary ui-button--large" to="/register">Start free <span aria-hidden="true">→</span></Link>
              <Link className="landing-v2__signin" to="/login">Sign in</Link>
            </div>
            <div className="landing-v2__reassurance"><span aria-hidden="true">✓</span> Easy to get started <span aria-hidden="true">·</span> Made for busy days</div>
          </FadeIn>
          <FadeIn className="landing-v2__hero-visual">
            <div className="landing-v2__visual-glow" aria-hidden="true" />
            <DashboardPreview />
          </FadeIn>
        </div>
      </section>

      <FadeIn as="section" inView className="landing-v2__section landing-v2__business" id="business-types">
        <div className="landing-v2__section-heading">
          <span className="landing-v2__eyebrow">MADE FOR YOUR KIND OF BUSINESS</span>
          <h2>Choose your business</h2>
          <p>Start with the tools that fit the way you work.</p>
        </div>
        <Stagger className="landing-v2__business-grid" inView>
          {businessTypes.map((business) => (
            <article className="landing-v2__business-card" key={business.title}>
              <span className="landing-v2__card-icon" aria-hidden="true">{business.icon}</span>
              <h3>{business.title}</h3>
              <p>{business.description}</p>
              <ul>{business.features.map((feature) => <li key={feature}><span aria-hidden="true">✓</span>{feature}</li>)}</ul>
            </article>
          ))}
        </Stagger>
      </FadeIn>

      <FadeIn as="section" inView className="landing-v2__section landing-v2__how">
        <div className="landing-v2__section-heading">
          <span className="landing-v2__eyebrow">A SIMPLE START</span>
          <h2>How it works</h2>
          <p>Get set up and get on with your day.</p>
        </div>
        <Stagger className="landing-v2__steps" inView>
          {steps.map((step) => (
            <article className="landing-v2__step" key={step.number}>
              <span className="landing-v2__step-number">{step.number}</span>
              <h3>{step.title}</h3>
              <p>{step.description}</p>
            </article>
          ))}
        </Stagger>
      </FadeIn>

      <FadeIn as="section" inView className="landing-v2__section landing-v2__features" id="features">
        <div className="landing-v2__section-heading">
          <span className="landing-v2__eyebrow">YOUR DAY, A LITTLE SIMPLER</span>
          <h2>Everything important, together</h2>
          <p>Clear tools for the work you do every day.</p>
        </div>
        <Stagger className="landing-v2__feature-grid" inView>
          {features.map((feature) => (
            <article className="landing-v2__feature-card" key={feature.title}>
              <span className="landing-v2__card-icon" aria-hidden="true">{feature.icon}</span>
              <h3>{feature.title}</h3>
              <p>{feature.description}</p>
            </article>
          ))}
        </Stagger>
      </FadeIn>

      <footer className="landing-v2__footer">
        <div className="landing-v2__footer-inner">
          <Brand />
          <div className="landing-v2__footer-cta">
            <div><h2>Make more room for your business.</h2><p>Start with a workspace designed to feel simple.</p></div>
            <Link className="ui-button ui-button--primary ui-button--large" to="/register">Start free <span aria-hidden="true">→</span></Link>
          </div>
          <div className="landing-v2__footer-bottom"><span>© OwnSpace. Built for your everyday.</span><Link to="/login">Sign in</Link></div>
        </div>
      </footer>
    </main>
  )
}
