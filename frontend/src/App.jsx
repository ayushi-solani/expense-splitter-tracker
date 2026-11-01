import { BrowserRouter, Routes, Route, Link } from 'react-router-dom'
import GroupsPage from './pages/GroupsPage.jsx'
import TrackerPage from './pages/TrackerPage.jsx'
import ForecastPage from './pages/ForecastPage.jsx'
import SettlementsPage from './pages/SettlementsPage.jsx'

export default function App() {
  return (
    <BrowserRouter>
      <nav>
        <Link to="/groups">Groups</Link>
        <Link to="/tracker">Tracker</Link>
        <Link to="/forecast">Forecast</Link>
        <Link to="/settlements">Settlements</Link>
      </nav>

      <Routes>
        <Route path="/groups" element={<GroupsPage />} />
        <Route path="/tracker" element={<TrackerPage />} />
        <Route path="/forecast" element={<ForecastPage />} />
        <Route path="/settlements" element={<SettlementsPage />} />
        <Route path="/" element={<TrackerPage />} />
      </Routes>
    </BrowserRouter>
  )
}
